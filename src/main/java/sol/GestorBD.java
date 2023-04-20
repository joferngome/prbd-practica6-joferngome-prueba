package sol;

import model.ExcepcionDeAplicacion;
import model.LineaEnRealizacion;
import model.PedidoEnRealizacion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

public class GestorBD extends bd.AbstractDBManager {

    @Override
    protected List<String> buscaArticulos(List<String> list, String s) throws ExcepcionDeAplicacion {
        //Declaración de las variables oportunas
        Connection con=null;
        List<String> articulos=new ArrayList<>();
        String concat = "AND";

        try{
            con = DriverManager.getConnection(URL, USR, PWD);


        /*Declaración de PreparedStatements/Statemens,
        En el caso de usar PreparedStatement, asignación de valores
        a los parámetros,
        Ejecución de las instrucciones de consulta*/
        /*Cierre de los recursos utilizados (ResultSets,
        PreparedStatements/Statements…*/

            String sql="SELECT codigo from articulo";
            if(s.toUpperCase().equals("OR")){
                concat = "OR";

            }




           if(list.size()>0){
               sql = sql+" WHERE ";


            for(String str: list){
                String aConcat = "upper(nombre) like upper('%"+str+"%') ";
                if(list.indexOf(str)+1<list.size()){
                    aConcat=aConcat+concat+" ";
                }

                sql = sql+aConcat;




            }
           }
           System.out.println(sql);
            Statement stm = con.createStatement();

           ResultSet res = stm.executeQuery(sql);


            while(res.next()){
                articulos.add(res.getString(1));



            }



            res.close();
            stm.close();


        } catch (SQLException exc) {
            exc.printStackTrace();
            throw new ExcepcionDeAplicacion("Error al recuperar articulos", exc);
        }
        finally {
            try {
                if (con != null) con.close();
            }catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        return articulos;



    }

    @Override
    public boolean isFestivo(Calendar calendar) throws ExcepcionDeAplicacion {
        //Declaración de las variables oportunas
        Connection con=null;
        boolean existe=false;



        try{
            con = DriverManager.getConnection(URL, USR, PWD);
            String sql = "Select fecha from festivo where fecha = { d  ?}";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            PreparedStatement ps = con.prepareStatement(sql);

            Date date = new Date(calendar.getTimeInMillis());
            ps.setString(1,sdf.format(date));
            ResultSet rs = ps.executeQuery();
            existe = rs.next();

            rs.close();
            ps.close();







        } catch (SQLException e) {
            e.printStackTrace();
            throw new ExcepcionDeAplicacion("Error al recuperar la fecha", e);
        }
        finally {
            try {
                if (con != null) con.close();
            }catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        return existe;



    }

    @Override
    public void aniadePedidoEnRealizacion(PedidoEnRealizacion pedidoEnRealizacion) throws ExcepcionDeAplicacion {
        Connection con = null;
        PreparedStatement stmE = null;
        PreparedStatement stmS = null;

        //Necesitamos insertar tantas lineasEnRealizacion como tenga el pedido


        try {


            con = DriverManager.getConnection(URL, USR, PWD);
            con.setAutoCommit(false); //Necesario contexto transaccional. Más de una tabla.



            //Primero insertamos el pedido porque hay dependencia para poder insertar lineasEnRealizacion
            String sqlE="INSERT INTO pedidoenrealizacion values(?,?,?)";
            stmE=con.prepareStatement(sqlE);
            stmE.setString(1, pedidoEnRealizacion.getCodigo());

            stmE.setString(3, pedidoEnRealizacion.getCliente().getCodigo());






            //Falta comprobar nulos de timeStamp
            Calendar fechaNcp=pedidoEnRealizacion.getInicio();
            Timestamp fechaNc=null;
            if(fechaNcp!=null){
                fechaNc= new Timestamp(fechaNcp.getTimeInMillis());

            }


            stmE.setTimestamp(2, fechaNc);


            stmE.executeUpdate();

            stmE.close();


                ///Parte de las Lineasenrealizacion

            //Fuera para poder reutilizarlo con el for.

            String sqlS="INSERT INTO lineaenrealizacion values(?,?,?,?,?,?)";

            for(LineaEnRealizacion lineaEnR : pedidoEnRealizacion.getLineas()){






                    stmS=con.prepareStatement(sqlS);
                    stmS.setString(1, pedidoEnRealizacion.getLinea(lineaEnR.getCodigo()).getCodigo());
                    stmS.setString(2, pedidoEnRealizacion.getLinea(lineaEnR.getCodigo()).getArticulo().getCodigo());
                    stmS.setInt(3, pedidoEnRealizacion.getLinea(lineaEnR.getCodigo()).getCantidad());
                    stmS.setDouble(4,pedidoEnRealizacion.getLinea(lineaEnR.getCodigo()).getPrecio());

                    Calendar fechaNcpp=pedidoEnRealizacion.getLinea(lineaEnR.getCodigo()).getFechaEntregaDeseada();
                    Timestamp fechaNcc=null;
                    if(fechaNcpp!=null){
                        fechaNcc= new Timestamp(fechaNcpp.getTimeInMillis());

                    }
                    stmS.setTimestamp(5,fechaNcc);

                    stmS.setString(6, pedidoEnRealizacion.getCodigo());


                    stmS.executeUpdate();

            }
                stmS.close();







            con.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException e2) {
                e2.printStackTrace();
                throw new ExcepcionDeAplicacion(e2);
            }
            throw new ExcepcionDeAplicacion(e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

        }



    }

    private static final String URL = getPropiedad("url");
    private static final String USR = getPropiedad("user");
    private static final String PWD = getPropiedad("password");
    //Métodos
    public static String getPropiedad(String clave) {
        String valor = null;
        try {
            Properties props = new Properties();
            InputStream prIS = GestorBD.class.getResourceAsStream("/conexion.properties");
            props.load(prIS);
            valor = props.getProperty(clave);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return valor;
    }
}
