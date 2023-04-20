package util;



import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlMetadataHandler;
import org.dbunit.ext.oracle.Oracle10DataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import sol.GestorBD;

public class TestsUtil {

	protected static GestorBD gbd;
	protected static String url;
	protected static String user;
	protected static String password;
	protected static String schema;


	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////// Metodos auxiliares//////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Cargamos en la BD los datos que necesitamos para hacer los tests
	public IDataSet readDataSet() throws Exception {

		FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
		builder.setColumnSensing(true);
		IDataSet dataSet = builder.build(new File("src/reducidopoblaciondatosiniciales.xml"));
		ReplacementDataSet dataSetR = new ReplacementDataSet(dataSet);
		dataSetR.addReplacementObject("[null]", null);
		

		return dataSetR;
	}

	public void cleanlyInsertDataset(IDataSet dataSet) {
		Connection jdbcConnection = null;
		IDatabaseConnection connection = null;
		try {
			jdbcConnection = DriverManager.getConnection(url, user, password);
			connection = new DatabaseConnection(jdbcConnection,schema);
			if(url.substring(5).startsWith("mysql"))
			{
				connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());
				connection.getConfig().setProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER, new MySqlMetadataHandler());
			}
			else {
				
				connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
			}
			connection.getConfig().setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
			connection.getConfig().setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);


	        
			DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DatabaseUnitException e) {
			e.printStackTrace();
		} finally {
			try {
				jdbcConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public ITable getTablaActual(String conexion, String user, String password, String esquema, String tabla) {
		Connection jdbcConnection = null;
		IDatabaseConnection connection = null;
		IDataSet databaseDataSet = null;
		ITable actualTable = null;
		try {
			jdbcConnection = DriverManager.getConnection(conexion, user, password);
			connection = new DatabaseConnection(jdbcConnection, esquema);
			DatabaseConfig config = connection.getConfig();
			if(conexion.substring(5).startsWith("mysql"))
			{
				config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());
				config.setProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER, new MySqlMetadataHandler());
			}
			else {
				
				config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new Oracle10DataTypeFactory());
			}
			config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
			config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
			databaseDataSet = connection.createDataSet();
			actualTable = databaseDataSet.getTable(tabla);
		} catch (DatabaseUnitException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				jdbcConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return actualTable;
	}

	public ITable getTablaEsperada(String tabla, String ficheroCarga) {
		IDataSet expectedDataSet = null;
		ITable expectedTable = null;
		try {
			expectedDataSet = new FlatXmlDataSetBuilder().build(new File(ficheroCarga));
			expectedTable = expectedDataSet.getTable(tabla);
		} catch (DataSetException e) {
			e.printStackTrace();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		return expectedTable;

	}

	public List<String> leerXML(String fichero, String nodoRaiz, String elemento) {
		List<String> listado = null;
		File xmlFile = new File(fichero);
		DocumentBuilderFactory factoriaDB = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = factoriaDB.newDocumentBuilder();
			Document doc;
			doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName(nodoRaiz);
			listado = new ArrayList<String>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				String valorAtributo = nodeList.item(i).getAttributes().item(0).getNodeValue();
				listado.add(valorAtributo);
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
		return listado;
	}
	
	
}

