package aspects;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

aspect JDBCAspect {
	private ThreadLocal<Throwable> lastLoggedException = new ThreadLocal<Throwable>();
	private Map<String, int[]> dataMap = new HashMap<String, int[]>();
	private String indent;
	private Logger _logger = Logger.getLogger(getClass());
	private int indentationlevel = 0;
	private int resultPS, resultS, resultRS, resultCSinC, resultCCC, resultConAUSinC, resultSinAUConC, resultConAUConC, resultGetConNombres, resultNotWasNull;

	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	/****************************************************************************************************/
	/***************************************
	 * Pointcuts definition
	 *****************************************/
	/****************************************************************************************************/
	/* Capturing exceptions in order to log them in an easy-to-understand format */
	protected pointcut exceptionTraced(): call(* *.*(..)) || execution( *.new(..)) && !within(JDBCAspect);

	/* Capturing the invocation of students' methods */
	protected pointcut methodTraced(): execution(public !static * *.GestorBD.*(..))  && execution(!static * *.GestorBD.*(..))  && !within(JDBCAspect) && !execution(* Object.*(..)) ;

	/*
	 * Capturing the invocation of JDBC API creation methods, closing of resources
	 * and invocation of setAutocommit/commmit
	 */
	pointcut create_PreparedStatement(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Connection.prepareStatement(..));

	pointcut close_PreparedStatement(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Statement.close()) && target(java.sql.PreparedStatement);

	pointcut create_Statement(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Connection.createStatement(..));

	pointcut close_Statement(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Statement.close()) && target(java.sql.Statement);

	pointcut execute_executeUpdate(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Statement+.executeUpdate(..));

	pointcut create_ResultSet(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Statement+.executeQuery(..));

	pointcut close_ResultSet(): withincode(* *.GestorBD.*(..)) && call(* java.sql.ResultSet.close());

	pointcut connection_autocommit(boolean b): withincode(* *.GestorBD.*(..)) && call(* java.sql.Connection.setAutoCommit(boolean)) && args(b) && if(!b);

	pointcut connection_commit(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Connection.commit());

	pointcut connection_close(): withincode(* *.GestorBD.*(..)) && call(* java.sql.Connection.close());

	pointcut connection_create(): withincode(* *.GestorBD.*(..)) && call(* java.sql.DriverManager.getConnection(..));

	// Usual errors: invocation of executeQuery method with a SQL statement from a
	// PreparedStatement
	pointcut misuse_of_executeQuery(String sql): withincode(* *.GestorBD.*(..)) && call(* java.sql.Statement.executeQuery(String)) && args(sql) && target(java.sql.PreparedStatement);

	// Usual errors: invocation of getXXX methods with a string argument
	pointcut misuse_of_getXXX(): withincode(* *.GestorBD.*(..)) && call(* java.sql.ResultSet.get*(String)) ;

	// Usual errors: capturing the invocation of getXXX methods (later in the
	// associated advice we check whether
	// such methods are invoked for primitive types) and of the wasNull method
	// (later we check and compare the number
	// of invocations to these methods.
	pointcut handling_null_values_getXXX(): withincode(* *.GestorBD.*(..)) && call(* java.sql.ResultSet.get*(*));

	pointcut handling_null_values_wasNull(): withincode(* *.GestorBD.*(..)) && call(* java.sql.ResultSet.wasNull());

	void around():call(* Exception.printStackDEBUG(..)){
		// So that if students use the printStachDEBUG method, it does not show any
		// message so that such messages do not interfere the aspect's logging messages
	}

	/*
	 * Per JDBC method, the following advices gather the required information
	 * regarding the number of: - PreparedStatements created and closed - Statements
	 * created and closed - ResultSets created and closed - Connections created and
	 * closed - invocations to setAutocommit/commit
	 */

	// aux[0] corresponds to the number of PreparedStatements created
	after(): create_PreparedStatement() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null){
			aux[0] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[1] corresponds to the number of PreparedStatements closed
	after(): close_PreparedStatement() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[1] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[2] corresponds to the number of Statements created
	after(): create_Statement() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[2] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[3] corresponds to the number of Statements closed
	after(): close_Statement() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[3] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[4] corresponds to the number of ResultSets created
	after(): create_ResultSet() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[4] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[5] corresponds to the number of ResultSets closed
	after(): close_ResultSet() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[5] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[6] corresponds to the number of Connections created
	after():connection_create(){
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[6] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[7] corresponds to the number of Connections closed
	after():connection_close(){
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[7] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[8] corresponds to the number of auto-commits invoked with "false"
	// argument
	after(boolean b):connection_autocommit(b){
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[8] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[9] corresponds to the number of invocations to the commit method
	after():connection_commit(){
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[9] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[10] corresponds to the number of invocations to getXXX methods with a
	// String as parameter
	after(): misuse_of_getXXX() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[10] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[11] corresponds to the number of invocations to getXXX methods with
	// primitive types
	after() returning (Object value): handling_null_values_getXXX() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			if (value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Integer
					|| value instanceof Long || value instanceof Float || value instanceof Double)
				aux[11] += 1;

			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}

	// aux[12] corresponds to the number of invocations to the wasNull method
	after() returning (Object value): handling_null_values_wasNull() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[12] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}
	// aux[13] corresponds to the number of invocations to the executeUpdate method
	after(): execute_executeUpdate() {
		int[] aux = dataMap.get(thisEnclosingJoinPointStaticPart.getSignature().getName());
		if (aux != null) {
			aux[13] += 1;
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), aux);
		}
	}


	// Advice to be executed if the executeQuery method has been invoked with a SQL
	// statement from a
	// PreparedStatement
	after(String sql): misuse_of_executeQuery(sql) {
		_logger.log(Level.INFO,
				indent + ANSI_YELLOW + "\t Revisa el codigo! Estas invocando al metodo executeQuery pasandole como parametro la instruccion SQL: "
						+ sql + " pero dicha instruccion ya la has proporcionado cuando has creado el PreparedStatement");

	}

	// Advice to be executed after throwing any exception during a student JDBC
	// method call
	after() throwing  (Throwable ex):exceptionTraced(){
		String methodName = thisJoinPointStaticPart.getSignature().getName();
		if (lastLoggedException.get() != ex) {
			lastLoggedException.set(ex);
			StackTraceElement[] frame = ex.getStackTrace();
			for (int i = 0; i < frame.length; i++) {
				if (frame[i].toString().contains("GestorBD")) {
					_logger.log(Level.INFO,
							indent + ANSI_RED + "\t Excepcion [Tienes un error en la linea " + frame[i].getLineNumber()
									+ " del metodo " + frame[i].getClassName() + "." + methodName + "("
									+ frame[i].getFileName() + ":" + frame[i].getLineNumber() + ")]");
					_logger.log(Level.INFO, indent + ANSI_RED + "\t\t   Error: " + ex.getMessage());

					break;
				}
			}
			dataMap.put(thisEnclosingJoinPointStaticPart.getSignature().getName(), new int[14]);
		}

	}

	//Advice to be executed "around" each traced method (each student JDBC method)
	Object around(): methodTraced(){
		// Configuration of indentation aspects so that the tracing information of each
		// JDBC method
		// is shown indented
		StringBuffer sb = new StringBuffer();
		if (_logger.isInfoEnabled()) {
			indentationlevel++;
			for (int i = 0, spaces = indentationlevel * 2; i < spaces; i++) {
				sb.append("  ");
			}
			indent = sb.toString();
			beforeLog(sb.toString(), thisJoinPoint);
		}
		Object result;
		try {
			// Call the proceed method to invoke the intercepted method
			result = proceed();
		} finally {
			// Configuration of indentation aspects so that the tracing information of each
			// JDBC method
			// is shown indented
			if (_logger.isInfoEnabled()) {

				afterLog(sb.toString(), thisJoinPoint);
				indentationlevel--;
			}
		}
		return result;
	}

	// Auxiliary method to be invoked from the previous "around" advice mainly to
	// show the name of the intercepted method and the arguments
	protected void beforeLog(String indent, JoinPoint joinPoint) {

		String name = joinPoint.getSignature().getName();
		// Initialization of the array associated to the JDBC method to be traced
		dataMap.put(name, new int[14]);
		_logger.log(Level.INFO,
				indent + ANSI_BLUE + "\t******************************** Metodo: " + name + "******************************");
		_logger.log(Level.INFO, createParameterMessage(indent, joinPoint));
		resultPS = -1;
		resultS = -1;
		resultRS = -1;
		resultCSinC = -1;
		resultCCC = -1;
		resultConAUSinC = -1;
		resultSinAUConC = -1;
		resultConAUConC = -1;
		resultGetConNombres = -1;
		resultNotWasNull = -1;

	}

	// Auxiliary method to be invoked from the previous "around" advice mainly to
	// show the report regarding not adopted best practices and motivational messages
	protected void afterLog(String indent, JoinPoint joinPoint) {
		String name = joinPoint.getSignature().getName();


		int[] aux = dataMap.get(joinPoint.getSignature().getName());
		if (aux != null) {
			if (aux[0] != 0 && aux[0] != aux[1]) {
				_logger.log(Level.INFO, indent
						+ ANSI_YELLOW + "\t Revisa el codigo! El numero de PreparedStatements creados no coincide con los que has cerrado ("
						+ aux[0] + " y " + aux[1] + ", respectivamente)");
				resultPS = 0;
			} else if (aux[0] != 0 && aux[0] == aux[1]) {
				_logger.log(Level.INFO, indent
						+ ANSI_GREEN + "\t Bien hecho!  Creas tantos PreparedStatements como los que cierras (" + aux[0] + ")");

				resultPS = 1;
			}
			if (aux[2] != 0 && aux[2] != aux[3]) {
				_logger.log(Level.INFO,
						indent + ANSI_YELLOW + "\t Revisa el codigo! El numero de Statements creados no coincide con los que has cerrado ("
								+ aux[2] + " y " + aux[3] + ", respectivamente).");
				resultS = 0;
			} else if (aux[2] != 0 && aux[2] == aux[3]) {
				_logger.log(Level.INFO,
						indent + ANSI_GREEN + "\t Bien hecho! Creas tantos Statements como los que cierras (" + aux[2] + ").");
				resultS = 1;
			}
			if (aux[4] != 0 && aux[4] != aux[5]) {
				_logger.log(Level.INFO,
						indent + ANSI_YELLOW + "\t Revisa el codigo!  El numero de ResultSets creados no coincide con los que has cerrado ("
								+ aux[4] + " y " + aux[5] + ", respectivamente).");
				resultRS = 0;
			} else if (aux[4] != 0 && aux[4] == aux[5]) {
				_logger.log(Level.INFO,
						indent + ANSI_GREEN + "\t Bien hecho! Creas tantos ResultSets como los que cierras  (" + aux[4] + ").");
				resultRS = 1;
			}
			if (aux[6] != 0 && aux[7] == 0) {
				_logger.log(Level.INFO, indent
						+ ANSI_YELLOW + "\t Revisa el codigo! Tu metodo JDBC crea una conexion con la BD pero, tras terminar de trabajar, no liberas el recurso.");
				resultCSinC = 0;

			} else if (aux[6] != 0 && aux[7] != 0) {
				_logger.log(Level.INFO,
						indent + ANSI_GREEN + "\t Bien hecho! Tu metodo JDBC crea una conexion con la BD y ademas, tras terminar de trabajar, liberas el recurso.");
				resultCSinC = 1;

			} else if (aux[6] == 0 && aux[7] != 0) {
				_logger.log(Level.INFO,
						indent + ANSI_YELLOW + "\t Revisa el codigo! Tu metodo JDBC toma prestada una conexion de otro metodo pero se la estas cerrando!!.");
				resultCCC = 0;

			} else if (aux[6] == 0 && aux[7] == 0 && (aux[0] != 0 || aux[2] != 0)) {
				_logger.log(Level.INFO, indent
						+ ANSI_GREEN + "\t Bien hecho! Tu metodo JDBC toma prestada una conexion de otro metodo y solamente la usas (no se la cierras).");
				resultCCC = 1;
			}
			if (aux[8] != 0 && aux[9] == 0) {
				_logger.log(Level.INFO, indent
						+ ANSI_YELLOW + "\t Revisa el codigo! Estableces el modo auto-commit a false pero al final no confirmas explicitamente la transaccion invocando al commit.");
				resultConAUSinC = 0;

			} else if (aux[8] == 0 && aux[9] != 0) {
				_logger.log(Level.INFO, indent
						+ ANSI_YELLOW + "\t Revisa el codigo! Confirmas la transaccion de forma explicita sin haber puesto previamente el modo auto-commit a false.");
				resultSinAUConC = 0;
			} else if (aux[8] != 0 && aux[9] != 0) {
				_logger.log(Level.INFO, indent
						+ ANSI_GREEN + "\t Bien hecho! Estableces el modo auto-commit a false y confirmas explicitamente la transaccion invocando al commit, te has asegurado de que invocas al rollback en el lugar adecuado?");
				resultConAUConC = 1;
			}
			if (aux[10] != 0) {
				_logger.log(Level.INFO, indent
						+ ANSI_YELLOW + "\t Revisa el codigo! Invocas a metodos del tipo getXXX utilizando los nombres de las columnas, lo cual no es una practica recomendada.");
				resultGetConNombres = 0;
			}
			if (aux[11] != aux[12]) {
				_logger.log(Level.INFO, indent
						+ ANSI_YELLOW + "\t Revisa el codigo para ver si necesitas corregirlo! (1) invocas a metodos del tipo getXXX siendo XXX tipos de datos primitivos, pero no compruebas la posibilidad de obtener valores nulos (wasNull)?, (2) usas wasNull cuando no es necesario?.");
				resultNotWasNull = 0;
			}
			if (aux[8] == 0 && aux[9] == 0 && aux[13] != 0) {
				_logger.log(Level.INFO, indent
						+ ANSI_GREEN + "\t En tu codigo ejecutas executeUpdate para realizar alguna operacion de modificacion. Debes analizar si necesitas establecer contexto transaccional (setAutoCommit a false, commit y rollback).");
				resultNotWasNull = 0;
			}
		}
		_logger.log(Level.INFO, indent + "\t******************************* Fin del metodo: " + name + "*****************************");

	}

	/* Logging arguments and managing indentations */
	private String createParameterMessage(String indent, JoinPoint joinPoint) {
		int num = 0;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		StringBuffer paramBuffer = new StringBuffer("");
		Object[] arguments = joinPoint.getArgs();
		paramBuffer.append(indent + "\tArgumentos: \n" + indent);
		for (int length = arguments.length, i = 0; i < length; ++i) {
			Object argument = arguments[i];
			num = i + 1;
			paramBuffer.append(indent + "    " + num + "- ");
			if (argument.getClass().getPackage().toString().contains("jdbc"))
				paramBuffer.append(argument.getClass().getSimpleName());
			else {
				if (argument instanceof Calendar)
					paramBuffer.append(sdf.format(((Calendar) argument).getTime()));
				else
					paramBuffer.append(argument);
			}
			if (i != length - 1) {
				paramBuffer.append("\n" + indent);
			}
		}
		return paramBuffer.toString();
	}

}
