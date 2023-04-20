package main;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.dbunit.Assertion;
import org.dbunit.assertion.comparer.value.ValueComparer;
import org.dbunit.assertion.comparer.value.ValueComparers;
import org.dbunit.assertion.comparer.value.builder.ColumnValueComparerMapBuilder;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementTable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import model.ExcepcionDeAplicacion;
import model.PedidoEnRealizacion;
import sol.GestorBD;
import util.TestsUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestsPr6 extends TestsUtil {

	@BeforeClass
	public static void creacionGestorBD() {
		gbd = new GestorBD();
		url = GestorBD.getPropiedad("url");
		user = GestorBD.getPropiedad("user");
		password = GestorBD.getPropiedad("password");
		schema = GestorBD.getPropiedad("schema");
	}

	// Antes de ejecutar cada test, eliminamos el estado previo de la BD, eliminando
	// los registros insertados en el test previo y cargando los datos requeridos
	// para dicho test.
	@Before
	public void importDataSet() throws Exception {
		IDataSet dataSet = readDataSet();
		cleanlyInsertDataset(dataSet);
	}

	@Test
	public void test1IsFestivoSi() {
		try {
			// 6-ene-2023 Festivo
			Calendar c1 = new GregorianCalendar(2023, 0, 6);
			assertTrue("Falla el comprobar test1IsFestivoSi", gbd.isFestivo(c1));
		} catch (ExcepcionDeAplicacion ex) {
			fail("Error comprobando festivos" + ex);
			ex.printStackTrace();
		}
	}

	@Test
	public void test2IsFestivoNo() {
		try {
			// 11-abr-2023 NO festivo
			Calendar c2 = new GregorianCalendar(2023, 3, 11);
			assertTrue("Falla el comprobar test1IsFestivoSNo", !gbd.isFestivo(c2));
		} catch (ExcepcionDeAplicacion ex) {
			fail("Error comprobando festivos" + ex);
			ex.printStackTrace();
		}
	}

	@Test
	public void test3Busquedas() {
		try {
			// Invoco al metodo buscaArticulos
			List<String> resultado = gbd.buscaArticulos("Lavadora fagor");
			// Cargo los datos esperados
			List<String> resultadoEsperado = leerXML("src/articulosesperados1.xml", "ARTICULO", "CODIGO");
			//Compruebo el resultado
			assertTrue(resultado.size() == resultadoEsperado.size() && resultado.containsAll(resultadoEsperado)
					&& resultadoEsperado.containsAll(resultado));
		} catch (ExcepcionDeAplicacion ex) {
			fail("Error buscando" + ex);
			ex.printStackTrace();
		}
	}

	@Test
	public void test4BusquedasMayusculasMinusculas() {
		try {
			// Invoco al metodo buscaArticulos
			List<String> resultado = gbd.buscaArticulos("LAVADORA Fagor");
			// Cargo los datos esperados
			List<String> resultadoEsperado = leerXML("src/articulosesperados1.xml", "ARTICULO", "CODIGO");
			//Compruebo el resultado
			assertTrue(resultado.size() == resultadoEsperado.size() && resultado.containsAll(resultadoEsperado)
					&& resultadoEsperado.containsAll(resultado));
		} catch (ExcepcionDeAplicacion ex) {
			fail("Error buscando" + ex);
			ex.printStackTrace();
		}
	}

	@Test
	public void test5BusquedasCero() {
		try {
			// Invoco al metodo buscaArticulos
			List<String> resultado = gbd.buscaArticulos("Lavadora lavavajillas fagor");
			//Compruebo el resultado
			assertTrue("Falla el comprobar test3BusquedasCero (deben ser 0 y devuelve " + resultado.size() + ")",
					resultado.isEmpty());
		} catch (ExcepcionDeAplicacion ex) {
			ex.printStackTrace();
			fail("Error buscando" + ex);
		}
	}

	@Test
	public void test6BusquedasAND() {
		try {
			// Invoco al metodo buscaArticulos
			List<String> resultado = gbd.buscaArticulos("Lavadora lavavajillas fagor", "AND");
			//Compruebo el resultado
			assertTrue("Falla el comprobar test3BusquedasAND (deben ser 0 y devuelve " + resultado.size() + ")",
					resultado.isEmpty());
		} catch (ExcepcionDeAplicacion ex) {
			fail("Error buscando" + ex);
			ex.printStackTrace();
		}
	}

	@Test
	public void test7BusquedasOR() {
		try {
			// Invoco al metodo buscaArticulos
			List<String> resultado = gbd.buscaArticulos("Lavadora lavavajillas fagor", "or");
			// Cargo los datos esperados
			List<String> resultadoEsperado = leerXML("src/articulosesperados2.xml", "ARTICULO", "CODIGO");
			//Compruebo el resultado
			assertTrue(resultado.size() == resultadoEsperado.size() && resultado.containsAll(resultadoEsperado)
					&& resultadoEsperado.containsAll(resultado));
		} catch (ExcepcionDeAplicacion ex) {
			fail("Error buscando" + ex);
			ex.printStackTrace(); 
		}
	}

	@Test
	public void test8InsertarPedidoEnRealizacion() throws Exception {
		// Aniadimos el pedido en realizacion, junto con sus lineas, invocando al metodo
		// aniadePedidoEnRealizacion
		PedidoEnRealizacion pr = new PedidoEnRealizacion(gbd.getCliente("070011/03"));
		Calendar fechaPedido = Calendar.getInstance();
		Calendar fechaDeseada = (Calendar) fechaPedido.clone();
		fechaDeseada.add(Calendar.DATE, 10);
		pr.addLinea(gbd.getArticulo("Mie/072CA"), 2, fechaDeseada);
		pr.addLinea(gbd.getArticulo("Fag/138MO"), 1, null);
		pr.addLinea(gbd.getArticulo("Ede/348FO"), 1, fechaDeseada);
		gbd.aniadePedidoEnRealizacion(pr);

		// Obtenemos de la BD el contenido de las tablas PedidoEnRealizacion y
		// LineaEnRealizacion tras la invocacion del metodo
		ITable tablaPedidoObtenida = getTablaActual(url, user, password, schema, "PEDIDOENREALIZACION");
		ITable tablaLineaObtenida = getTablaActual(url, user, password, schema, "LINEAENREALIZACION");

		// Cargamos los datos esperados del XML
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date fechaPedidoDate = fechaPedido.getTime();
		ITable tablaPedidoEsperada = getTablaEsperada("pedidoenrealizacion", "src/pedidosesperados.xml");
		ReplacementTable tablaPedidoEsperadaReemplazada = new ReplacementTable(tablaPedidoEsperada);
		tablaPedidoEsperadaReemplazada.addReplacementObject("[TODAY]", sdf.format(fechaPedidoDate));

		ITable tablaLineaEsperada = getTablaEsperada("lineaenrealizacion", "src/lineasesperadas.xml");
		Date fechaDeseadaDate = fechaDeseada.getTime();
		ReplacementTable tablaLineaEsperadaReemplazada = new ReplacementTable(tablaLineaEsperada);
		tablaLineaEsperadaReemplazada.addReplacementObject("[DESIREDDATE]", sdf.format(fechaDeseadaDate));
		tablaLineaEsperadaReemplazada.addReplacementObject("[NULL]", null);


		ValueComparer comparadorValores = ValueComparers.isActualEqualToExpected;
		Map<String, ValueComparer> comlumnasComparadorValores = new ColumnValueComparerMapBuilder()
				.add("FECHAINICIO", ValueComparers.isActualEqualToExpectedTimestampWithIgnoreMillis)
				.add("FECHAENTREGADESEADA", ValueComparers.isActualEqualToExpectedTimestampWithIgnoreMillis).build();
		
		// Comprobamos que el contenido actual de ambas tablas en la BD coincide
		// con las tablas esperadas cargadas en los XML
		Assertion.assertWithValueComparer(tablaPedidoEsperadaReemplazada, tablaPedidoObtenida, comparadorValores,
				comlumnasComparadorValores);
		Assertion.assertWithValueComparer(tablaLineaEsperadaReemplazada, tablaLineaObtenida, comparadorValores,
				comlumnasComparadorValores);
	}
}
