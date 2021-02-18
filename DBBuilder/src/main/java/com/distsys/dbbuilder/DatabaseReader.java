/*
 *
 *
 */

package com.distsys.dbbuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 21/10/2010 21:34:54
 * @author Murilo
 */
public class DatabaseReader {

    List<Tabela> reversa() throws SQLException {
        List<Tabela> tabs = new ArrayList<Tabela>();
        Connection conn = ConnectionFactory.getConnection();
        Statement stmt = conn.createStatement();
        String query = null;
        String sgbd = System.getProperty("sgbd");
        if("sqlserver".equals(sgbd)) {
            query = "select name from sysobjects where type='U'";
        } else if("oracle".equals(sgbd)) {
            query = "select OBJECT_NAME from user_objects where object_type = 'TABLE'";
        } else {
            query = "show tables";
        }

        ResultSet rs = stmt.executeQuery(query);
        while(rs.next()) {
            Tabela t = new Tabela();
            t.setNome(rs.getString(1));
            String nomeVar = Main.nomeSqlParaNomeJava(t.getNome());
            t.setNomeClasse(nomeVar.substring(0, 1).toUpperCase()+nomeVar.substring(1));
            if("sqlserver".equals(sgbd)) {
                preencheColunasSqlServer(t);
            } else {
                preencheColunasMySQL(t);
            }
            
            tabs.add(t);
        }

        rs.close();
        stmt.close();
        ConnectionFactory.devolver(conn);
        return tabs;
    }

    private void preencheColunasSqlServer(Tabela t) throws SQLException {
//        String query = "SELECT table_name,ordinal_position,column_name,data_type,"
//                + "\nis_nullable,character_maximum_length"
//                + "\nFROM information_schema.COLUMNS"
//                + "\nWHERE table_name = '"+t.getNome()+"'"
//                + "\nORDER BY ordinal_position";
        String query = "SELECT     sysobjects.name AS table_name, syscolumns.name AS column_name, systypes.name AS datatype, syscolumns.length AS length"
                + "\nFROM         sysobjects INNER JOIN"
                + "\nsyscolumns ON sysobjects.id = syscolumns.id INNER JOIN"
                + "\nsystypes ON syscolumns.xtype = systypes.xtype"
                + "\nWHERE     (sysobjects.xtype = 'U' and sysobjects.name='" + t.getNome() + "')"
                + "\nORDER BY sysobjects.name, syscolumns.colid";
        Connection conn = ConnectionFactory.getConnection();
        Statement stmt = conn.createStatement();
        System.out.println(query);
        System.out.flush();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {    
            //String cname = rs.getString("column_name");
            String cname = rs.getString(2);
            Coluna coluna = new Coluna(cname);
//            String tipo = rs.getString("data_type");
            String tipo = rs.getString(3);
            coluna.setTipo(tipo);
//            int tamanho = rs.getInt("character_maximum_length");
            int tamanho = rs.getInt(4);
            if(!rs.wasNull() && tamanho>0) {
                coluna.setTamanho(tamanho);
            }
            t.addColuna(coluna);
        }
        rs.close();
        stmt.close();
        ConnectionFactory.devolver(conn);
    }

    private void preencheColunasMySQL(Tabela t) throws SQLException {
        String query = "DESC "+t.getNome();
        if("oracle".equals(System.getProperty("sgbd"))) {
            query = "SELECT column_name \"Field\", nullable \"Null\", concat(concat(concat(data_type,'('),data_length),')') \"Type\""
                    + "\nFROM user_tab_columns"
                    + "\nWHERE table_name='"+t.getNome()+"'";
        }
        System.out.println(query);
        System.out.flush();
        Connection conn = ConnectionFactory.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while(rs.next()) {
            String cname = rs.getString("Field");
            Coluna coluna = new Coluna(cname);
            String tipo = rs.getString("Type");
            Boolean nullable = rs.getBoolean("Null");

            int tamanho = 0;
            if(tipo.toLowerCase().startsWith("enum")) {
                
            } else if(tipo.contains("(")) {
                int i = tipo.indexOf("(");
                int j = tipo.indexOf(")");
                if (i < 0 || j <= 0) {
                    System.out.println("Vai dar erro");
                }
                String tamanhoStr = tipo.substring(i + 1, j);

                if(tipo.toLowerCase().startsWith("enum") || tipo.toLowerCase().startsWith("set")) {
                    String[] enuns = tamanhoStr.replaceAll("'", "").replaceAll(" ", "").split(",");
                    coluna.setTipoEnum(enuns);
                } else {
                    
                    if (tamanhoStr.contains(",")) {
                        int k = tamanhoStr.indexOf(",");
                        tamanho = Integer.parseInt(tamanhoStr.substring(0, k));
                        tamanho += Integer.parseInt(tamanhoStr.substring(k + 1));
                    } else {
                        tamanho = Integer.parseInt(tamanhoStr);
                    }
                }

                tipo = tipo.substring(0, tipo.indexOf("("));
            }
            if(tamanho>0) {
                coluna.setTamanho(tamanho);
            }
            coluna.setNullable(nullable);
            coluna.setTipo(tipo);
            t.addColuna(coluna);
        }
        rs.close();
        stmt.close();
        ConnectionFactory.devolver(conn);
    }

}
