package com.distsys.dbbuilder;

import com.thoughtworks.xstream.XStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * -Dsgbd=oracle -Duser=orami -Dpassword=me6a1nfra99 -Dhost=localhost
 * -Ddatabase=XE
 *
 * @author David Ohio
 */
public class Main {

    private static final String BASEDIR = "out";
    private static final Set<String> messages = new TreeSet<String>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("directory or database");
            System.out.println("directory <dir>");
            System.out.println("database sgbd server databasename");
            return;
        } else {
            if ("directory".equalsIgnoreCase(args[0])) {
                if (args.length < 2) {
                    System.out.println("which directory");
                    return;
                }

                File f = new File(args[1]);
                if (f.exists() && f.isDirectory()) {
                    importar(f);
                } else {
                    System.out.println("Diretório não encontrado: " + f.getAbsolutePath());
                }
            } else if ("database".equalsIgnoreCase(args[0])) {
                try {
                    //                if (args.length < 4) {
                    //                    System.out.println("which disgbd server databasenameectory");
                    //                    return;
                    //                }
                    configureConnection();
                    DatabaseReader dr = new DatabaseReader();
                    List<Tabela> tabelas = dr.reversa();
                    resultado(tabelas);
                } catch (SQLException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Erro ao fazer a engenharia reversa", ex);
                }
            }
        }
    }

    private static void importar(File f) {
        File[] fs = f.listFiles();
        System.out.println(fs.length + " arquivos");
        List<Tabela> tabelas = new ArrayList<Tabela>();
        for (File file : fs) {
            Tabela tabela = criaTabela(file);
            if (tabela != null) {
                tabelas.add(tabela);
            }
        }
//        exibeEmXml(tabelas);
        adivinhaEstrutura(tabelas);
        resultado(tabelas);
    }

    private static Tabela criaTabela(File file) {
        FileReader fr = null;
        try {
            fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String aux = br.readLine();
            String campos[] = aux.split("\t");

            Tabela t = new Tabela();
            t.setNome(file.getName().replace(".txt", ""));

            for (String campo : campos) {
                campo = campo.replaceAll("\"", "");
                t.addColuna(campo);
            }

            //Trabalhando com um vetor por que sabemos
            //o tamanho, internamente trabalharemos
            //com uma lista por que precisamos da
            //ordem correta e nao sabemos quantas linhas
            //tem cada arquivo
            Object valores[] = new Object[campos.length];
            for (int i = 0; i < valores.length; i++) {
                List<String> valoresInternos = new ArrayList<String>();
                valores[i] = valoresInternos;
            }

            t.setValores(valores);

            while ((aux = br.readLine()) != null) {
                campos = aux.split("\t");
                //Cada linha tem um valor de cada coluna
                for (int i = 0; i < campos.length; i++) {
                    String campo = campos[i];
                    campo = campo.replaceAll("\"", "");
                    //filtrado o valor do campo
                    //adiciona-se o valor na enessima coluna
                    ((List<String>) valores[i]).add(campo);
                }
            }

            br.close();
            fr.close();
            return t;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private static void exibeEmXml(List<Tabela> tabelas) {
        XStream xstream = new XStream();
        xstream.autodetectAnnotations(true);
        xstream.processAnnotations(Tabela.class);
        xstream.alias("tabela", Tabela.class);
//        for (Tabela t : tabelas) {
//            System.out.println(xstream.toXML(t));
//        }
        System.out.println(xstream.toXML(tabelas));
    }

    private static void adivinhaEstrutura(List<Tabela> tabelas) {
        for (Tabela tabela : tabelas) {
            List<Coluna> colunas = tabela.getColunas();
            Object[] valoresColunas = tabela.getValores();

            for (int i = 0; i < colunas.size(); i++) {
                List<String> valores = (List<String>) valoresColunas[i];
                adivinhaEstrutura(colunas.get(i), valores);
            }
        }
    }

    private static void adivinhaEstrutura(Coluna coluna, List<String> valores) {
        int maiorTamanho = 0;

//        proximo: for(String valor:valores) {
//            if(valor!=null && !valor.isEmpty()) {
//                if(maiorTamanho<valor.length()) {
//                    maiorTamanho = valor.length();
//                }
//                char[] cs = valor.toCharArray();
//                for(char c:cs) {
//                    Character.isLetter(c);
//                    continue proximo;
//                }
//            }
//        }

        boolean letras = false;
        boolean numeros = false;
        int barras = 0;

        boolean doisPontos = false;
//        boolean ponto = false;
        boolean virgula = false;
        boolean possuiEspaco = false;

        for (String valor : valores) {
            if (valor != null && !valor.isEmpty()) {
                if (maiorTamanho < valor.length()) {
                    maiorTamanho = valor.length();
                }
                char[] cs = valor.toCharArray();
                for (char c : cs) {
                    if (Character.isLetter(c)) {
                        letras = true;
                        break;
                    }
                    if (Character.isDigit(c)) {
                        numeros = true;
                    }
                    if ('/' == c) {
                        barras++;
                    }
                    if (':' == c) {
                        doisPontos = true;
                    }
                    if (',' == c) {
                        virgula = true;
                    }
                    if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                        possuiEspaco = true;
                    }
                }
            }
        }
        if (valores.size() > 0) {
            barras = barras / valores.size();
        }

        if (!letras) {
            if ((barras == 2) && numeros) {
                if (possuiEspaco && doisPontos) {
                    coluna.setTipo("DATETIME");
                } else {
                    coluna.setTipo("DATE");
                }
            } else if (numeros) {
                if (virgula) {
                    coluna.setTipo("DOUBLE(11,2)");
                } else {
                    coluna.setTipo("INT");
                }
            }
        }

        if (coluna.getTipo() == null || "".equals(coluna.getTipo())) {
            coluna.setTipo("VARCHAR");
        }
        coluna.setTamanho(maiorTamanho);
    }

    private static void geraDDL(List<Tabela> tabelas, StringBuilder sb) {
        for (Tabela tabela : tabelas) {
            sb.append("\nDROP TABLE IF EXISTS ");
            sb.append(tabela.getNome());
            sb.append(";\n");
            sb.append("CREATE TABLE ");
            sb.append(tabela.getNome());
            sb.append(" (");

            boolean primeiro = true;
            for (Coluna coluna : tabela.getColunas()) {
                if (primeiro) {
                    primeiro = false;
                } else {
                    sb.append(",");
                }
                sb.append("\n");
                sb.append(coluna.getNome());
                sb.append(" ");
                String tipo = coluna.getTipo();
                sb.append(tipo);
                if ("VARCHAR".equals(tipo) || "INT".equals(tipo)) {
                    sb.append("(");
                    sb.append(coluna.getTamanho());
                    sb.append(")");
                }
            }
            sb.append("\n);");
        }
    }

    private static void geraSQL(List<Tabela> tabelas, StringBuilder sbt) {
        SimpleDateFormat brDateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat brDateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sqlDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        for (Tabela tabela : tabelas) {
            if (tabela.getValores() == null || tabela.getValores().length == 0) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ");
            sb.append(tabela.getNome());
            sb.append(" (");

            boolean primeiro = true;
            for (Coluna coluna : tabela.getColunas()) {
                if (primeiro) {
                    primeiro = false;
                } else {
                    sb.append(",");
                }
                sb.append(coluna.getNome());
            }
            sb.append(") VALUES (");
            Object[] valores = tabela.getValores();
            List<String> ultimaColuna = (List<String>) valores[valores.length - 1];
            //ultimaColuna
            for (int i = 0; i < ultimaColuna.size(); i++) {
                sbt.append(sb);
                for (int j = 0; j < valores.length; j++) {
                    if (j > 0) {
                        sbt.append(",");
                    }
                    List<String> coluna = (List<String>) valores[j];
                    Coluna colunaModel = tabela.getColunas().get(j);

                    String celula = coluna.get(i);
                    if (celula != null && !"".equals(celula)) {
                        try {
                            if (colunaModel.getTipo().equals("DOUBLE(11,2)")) {
                                celula = celula.replace(",", ".");
                            } else if (colunaModel.getTipo().equals("DATETIME")) {
                                celula = sqlDateTimeFormat.format(brDateTimeFormat.parse(celula));
                            } else if (colunaModel.getTipo().equals("DATE")) {
                                celula = sqlDateFormat.format(brDateFormat.parse(celula));
                            }
                        } catch (ParseException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Tabela "
                                    + tabela.getNome() + " Coluna : " + colunaModel.getNome() + " Linha: " + i, ex);
                        }
                        celula = "\"" + celula + "\"";
                        sbt.append(celula);
                    } else {
                        sbt.append("null");
                    }
                }
                sbt.append(");\n");
            }
        }
    }

    private static List<StringBuilder> geraModelos(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<>();
        for (Tabela tab : tabelas) {
            sbs.add(criaModelo(tab));
        }
        return sbs;
    }
    
    private static List<StringBuilder> geraModelosBuilder(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<>();
        for (Tabela tab : tabelas) {
            sbs.add(criaModeloBuilder(tab));
        }
        return sbs;
    }

    private static List<StringBuilder> geraModelosAtivos(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<>();
        for (Tabela tab : tabelas) {
            sbs.add(criaModeloAtivo(tab));
        }
        return sbs;
    }
    
    private static StringBuilder criaModeloAtivo(Tabela tab) {
        StringBuilder modelo = new StringBuilder();
        modelo.append("package modelActive;");
        modelo.append("\n");
        modelo.append("public class ");
        modelo.append(tab.getNomeClasse());
        modelo.append("Model {");

        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\tprivate ");
            String tipoJava = tipoSqlParaJava(coluna);
            coluna.setTipoJava(tipoJava);
            modelo.append(tipoJava);
            modelo.append(" ");

            String nomeJava = nomeSqlParaNomeJava(coluna.getNome());
            coluna.setNomeJava(nomeJava);
            modelo.append(nomeJava);
            modelo.append(";");
            modelo.append("\n\tprivate boolean ");
            modelo.append(nomeJava);
            modelo.append("Updated = false;");
        }
        modelo.append("\n");

        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\tpublic ");
            modelo.append(coluna.getTipoJava());
            modelo.append(" get");
            modelo.append(coluna.getNomeJavaParaMetodo());
            modelo.append("() {\n\t\treturn this.");
            modelo.append(coluna.getNomeJava());
            modelo.append(";\n\t}\n");

            modelo.append("\n\tpublic void set");
            modelo.append(coluna.getNomeJavaParaMetodo());
            modelo.append(" (");
            modelo.append(coluna.getTipoJava());
            modelo.append(" ");
            modelo.append(coluna.getNomeJava());
            modelo.append(") {\n\t\tif(this.");
            modelo.append(coluna.getNomeJava());
            if (coluna.getTipoJava().equals("String")) {
                modelo.append("!=null && !this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(".equals(");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else {
                modelo.append(" != ");
                modelo.append(coluna.getNomeJava());
            }
            modelo.append(") {\n\t\t\tthis.");
            modelo.append(coluna.getNomeJava());
            modelo.append(" = ");
            modelo.append(coluna.getNomeJava());
            modelo.append(";\n\t\t\tthis.");
            modelo.append(coluna.getNomeJava());
            modelo.append("Updated = true;\n\t\t}");
            modelo.append("\n\t}\n");
        }

        modelo.append("\n\tprivate void setValues(ResultSet rs) throws SQLException {\n");
        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\t\tthis.");
            modelo.append(coluna.getNomeJava());
            modelo.append(" = rs.get");
            modelo.append(nomeTipoJdbc(coluna));
            modelo.append("(\"");
            modelo.append(coluna.getNome());
            modelo.append("\");\n\t\t");

            modelo.append("this.");
            modelo.append(coluna.getNomeJava());
            modelo.append("Updated = false;");
        }
        modelo.append("\n\t}\n");
        
        modelo.append("\n\tprivate void clear() {\n");
        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\t\tthis.");
            modelo.append(coluna.getNomeJava());
            modelo.append(" = ");
            if ("String".equalsIgnoreCase(coluna.getTipoJava())) {
                modelo.append("\"\";");
            } else if("Double".equalsIgnoreCase(coluna.getTipoJava())) {
                modelo.append("0d;");
            } else if("Float".equalsIgnoreCase(coluna.getTipoJava())) {
                modelo.append("0f;");
            } else {
                modelo.append("0;");
            }
            modelo.append("\n\t\t");
            
            modelo.append("this.");
            modelo.append(coluna.getNomeJava());
            modelo.append("Updated = false;");
        }
        modelo.append("\n\t}\n");
        
        modelo.append("\n\tpublic String getSelect() {\n");
        modelo.append("        return \"SELECT ");
        String prefixo = tab.getNome().substring(0, 1);
        
        boolean first = true;
        for (Coluna coluna : tab.getColunas()) {
            if(first) {
                first = false;
            } else {
                modelo.append(",");
            }
            modelo.append(prefixo).append(".").append(coluna.getNome());
        }
        modelo.append("\"\n");        
        
        modelo.append("            + \"\\nFROM ");
        modelo.append(tab.getNome());
        modelo.append(" ").append(prefixo).append("\";\n\t}");
        
        modelo.append("\n\tpublic String getInsert() {\n");
        modelo.append("        String retorno = \"INSERT INTO ");
        modelo.append(tab.getNome()).append(" (\";\n");
        modelo.append("        StringBuilder columns = new StringBuilder(\"CADASTRO, ALTERADO\");\n");
        modelo.append("        StringBuilder values = new StringBuilder(\"NOW(), NOW()\");");
        
        for (Coluna coluna : tab.getColunas()) {
            if("CADASTRO".equalsIgnoreCase(coluna.getNome()) || "ALTERADO".equalsIgnoreCase(coluna.getNome())) {
                continue;
            }
            modelo.append("\n\t\tif (").append(coluna.getNomeJava()).append("Updated) {\n");
            modelo.append("            columns.append(\", ").append(coluna.getNome()).append("\");\n");
            modelo.append("            values.append(\", \\\"\");\n");
            modelo.append("            values.append(");
            if("String".equalsIgnoreCase(coluna.getTipoJava())) {
                modelo.append("StringUtil.prepareString(");
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else if("DATE".equalsIgnoreCase(coluna.getTipo())) {
                modelo.append("TimeUtil.getShortDateForBd(");
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else if("DATETIME".equalsIgnoreCase(coluna.getTipo())
                    || "TIMESTAMP".equalsIgnoreCase(coluna.getTipo())) {
                modelo.append("TimeUtil.getShortDateTimeForBd(");
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else {
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
            }
            modelo.append(");\n");
            modelo.append("            values.append(\"\\\"\");\n");
            modelo.append("        }\n");
        }
        modelo.append("\n\t\tretorno += columns.toString() + \") VALUES (\" + values.toString() + \")\";\n");
        modelo.append("        return retorno;\n");
        modelo.append("    }");

        
        modelo.append("\n\tpublic  String getUpdate() {");
        modelo.append("\n\t\tStringBuilder retorno = new StringBuilder(\"UPDATE ");
        modelo.append(tab.getNome());
        modelo.append(" (\");");
        modelo.append("\n\t\tboolean firstColumn = true;");
        for (Coluna coluna : tab.getColunas()) {
            if("CADASTRO".equalsIgnoreCase(coluna.getNome()) || "ALTERADO".equalsIgnoreCase(coluna.getNome())) {
                continue;
            }
            modelo.append("\n\t\t\tif (");
            modelo.append(coluna.getNomeJava());
            modelo.append("Updated) {"
                        + "\n\t\t\t\tif (firstColumn) {"
                        + "\n\t\t\t\t\tfirstColumn = false;"
                        + "\n\t\t\t\t} else {"
                        + "\n\t\t\t\t\tretorno.append(\",\");"
                        + "\n\t\t\t\t}"
                        +"\n\t\t\tretorno.append(\"");
            modelo.append(coluna.getNome());
            modelo.append("=\\\"\").append(");
            if(coluna.getTipoJava().equals("String")) {
                modelo.append("StringUtil.prepareString(this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else if("DATE".equalsIgnoreCase(coluna.getTipo())) {
                modelo.append("TimeUtil.getShortDateForBd(");
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else if("DATETIME".equalsIgnoreCase(coluna.getTipo())
                    || "TIMESTAMP".equalsIgnoreCase(coluna.getTipo())) {
                modelo.append("TimeUtil.getShortDateTimeForBd(");
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
                modelo.append(")");
            } else {
                modelo.append("this.");
                modelo.append(coluna.getNomeJava());
            }
            
            modelo.append(").append(\"\\\"\");\n\t\t}");
        }
        modelo.append("\n\t\tif (!firstColumn) {");
        modelo.append("\n\t\t\tretorno.append(\"ALTERADO=NOW()\");\n\t\t}");

        modelo.append("\n\t\tretorno.append(\" WHERE \");");
        modelo.append("\n\t\treturn retorno.toString();");
        modelo.append("\n\t}");
        
        modelo.append("}");
        return modelo;
    }

    private static StringBuilder criaModelo(Tabela tab) {
        StringBuilder modelo = new StringBuilder();
        modelo.append("package model;\n");
        modelo.append("import java.io.Serializable;\n");
        modelo.append("import java.util.Date;\n");
        modelo.append("\n");
        modelo.append("public class ");
        modelo.append(tab.getNomeClasse());
        modelo.append(" implements Serializable {");

        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\tprivate ");
            String tipoJava = tipoSqlParaJava(coluna);
            coluna.setTipoJava(tipoJava);
            modelo.append(tipoJava);
            modelo.append(" ");

            String nomeJava = nomeSqlParaNomeJava(coluna.getNome());
            coluna.setNomeJava(nomeJava);
            modelo.append(nomeJava);
            modelo.append(";");
        }
        modelo.append("\n");

        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\tpublic ");
            modelo.append(coluna.getTipoJava());
            modelo.append(" get");
            modelo.append(coluna.getNomeJavaParaMetodo());
            modelo.append("() {\n\t\treturn this.");
            modelo.append(coluna.getNomeJava());
            modelo.append(";\n\t}\n");

            modelo.append("\n\tpublic void set");
            modelo.append(coluna.getNomeJavaParaMetodo());
            modelo.append(" (");
            modelo.append(coluna.getTipoJava());
            modelo.append(" ");
            modelo.append(coluna.getNomeJava());
            modelo.append(") {\n\t\tthis.");
            modelo.append(coluna.getNomeJava());
            modelo.append(" = ");
            modelo.append(coluna.getNomeJava());
            modelo.append(";\n\t}\n");
        }

        modelo.append("}");
        return modelo;
    }
    
    private static StringBuilder criaModeloBuilder(Tabela tab) {
        StringBuilder modelo = new StringBuilder();
        modelo.append("package builders;\n");
        modelo.append("\n");
        modelo.append("import java.util.Date;\n");
        modelo.append("\n");
        modelo.append("public class ");
        modelo.append(tab.getNomeClasse());
        modelo.append("Builder {");

        modelo.append("\n\n\tprivate ");
        modelo.append(tab.getNomeClasse());
        modelo.append(" ");
        modelo.append(tab.getNomeVariavel());
        modelo.append(" = new ");
        modelo.append(tab.getNomeClasse());
        modelo.append("();");
        
        modelo.append("\n\n\tpublic void novo() {\n\t\tthis.");
        modelo.append(tab.getNomeVariavel());
        modelo.append(" = new ");
        modelo.append(tab.getNomeClasse());
        modelo.append("();\n\t}");
        
        modelo.append("\n\n\tpublic ");
        modelo.append(tab.getNomeClasse());
        modelo.append(" build() {\n\t\treturn ");
        modelo.append(tab.getNomeVariavel());
        modelo.append(";\n\t}\n");

        for (Coluna coluna : tab.getColunas()) {
            modelo.append("\n\tpublic ");
            
            modelo.append(tab.getNomeClasse());
            modelo.append("Builder with");
            modelo.append(coluna.getNomeJavaParaMetodo());
            modelo.append("(");
            modelo.append(coluna.getTipoJava());
            modelo.append(" ");
            modelo.append(coluna.getNomeJava());
            modelo.append(") {");

            modelo.append("\n\t\t").append(tab.getNomeVariavel());
            modelo.append(".set").append(primeiraLetraMaiuscula(coluna.getNomeJava()));
            modelo.append(coluna.getNomeJava());
            modelo.append("(");
            modelo.append(");");
            modelo.append("\n\t\treturn this;");
            modelo.append("\n\t}\n");
        }

        modelo.append("}");
        return modelo;
    }

    private static String tipoSqlParaJava(Coluna coluna) {
        String tipou = coluna.getTipo().toUpperCase();
        int tamanho = coluna.getTamanho();
        if (tipou.equals("BIT") || tipou.equals("TINYINT") || tipou.equals("TINYINT UNSIGNED")) {
            return "boolean";
        }
        if (tipou.contains("BINARY") || tipou.equals("IMAGE")
                || tipou.equals("BLOB") || tipou.equals("MEDIUMBLOB")
                || tipou.equals("LONGBLOB") || tipou.equals("RAW")) {
            return "byte[]";
        }
        if (tipou.startsWith("DATE") || tipou.startsWith("TIME")
                || tipou.equals("SMALLDATETIME")) {
            return "Date";
        }
        if (tipou.equals("INT") || tipou.equals("SMALLINT") || tipou.equals("SMALLINT UNSIGNED")
                || tipou.equals("INT UNSIGNED")) {
            if (coluna.isNullable()) {
                return "Integer";
            } else {
                return "int";
            }
        }
        if (tipou.equals("BIGINT")
                || tipou.equals("BIGINT UNSIGNED")
                //|| (tipou.equals("NUMBER") && (tamanho==0 || tamanho==1))) {
                || tipou.equals("NUMBER")) {
            if (coluna.isNullable()) {
                return "Long";
            } else {
                return "long";
            }
        }
        if (tipou.startsWith("DOUBL") || tipou.startsWith("FLOA")
                || tipou.equals("MONEY") || tipou.equals("REAL")
                || tipou.equals("DECIMAL") || tipou.equals("NUMERIC")) {
            //|| tipou.equals("NUMBER")) {
            if (coluna.isNullable()) {
                return "Double";
            } else {
                return "double";
            }
        }
        if (tipou.startsWith("VARCH") || tipou.startsWith("CHA")
                || tipou.startsWith("NVARCHAR") || tipou.equals("TEXT")
                || tipou.equals("LONGTEXT") || tipou.equals("MEDIUMTEXT")
                || tipou.equals("ENUM") || tipou.startsWith("SET")
                || tipou.equals("SYSNAME") || tipou.equals("NCHAR")
                || tipou.equals("NCHAR") || tipou.equals("XML")
                 || tipou.equals("JSON")) {
            return "String";
        }
        if (tipou.toLowerCase().startsWith("enum")) {
            return tipou;
        }
        throw new UnsupportedOperationException("Não implementamos a função para " + tipou);
    }

    static String nomeSqlParaNomeJava(String nome) {
        if (nome.length() > 1) {
            nome = nome.toLowerCase();
            while (nome.contains("_")) {
                int idx = nome.indexOf("_");
                String aux = nome.substring(0, idx);
                aux += nome.substring((idx + 1), (idx + 2)).toUpperCase();
                aux += nome.substring(idx + 2);
                nome = aux;
            }
        } else {
            nome = nome.toLowerCase();
        }

        return nome;
    }

    private static List<StringBuilder> geraDaos(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<StringBuilder>();
        for (Tabela tab : tabelas) {
            sbs.add(criaDao(tab));
        }
        return sbs;
    }

    private static StringBuilder criaDao(Tabela tab) {
        StringBuilder modelo = new StringBuilder();
        modelo.append("package dao;\n");
        modelo.append("import br.com.jcomputacao.dao.ChavePrimariaDescritor;\n");
        modelo.append("import br.com.jcomputacao.dao.Dao;\n");
        modelo.append("import java.sql.PreparedStatement;\n");
        modelo.append("import java.sql.ResultSet;\n");
        modelo.append("import java.sql.SQLException;\n");
        modelo.append("import model.");
        modelo.append(tab.getNomeClasse());
        modelo.append(";\n\n");
        modelo.append("public class ");
        modelo.append(tab.getNomeClasse());
        modelo.append("Dao extends Dao<");
        modelo.append(tab.getNomeClasse());
        modelo.append("> {\n");

        modelo.append("\n"
                + "    private static final ChavePrimariaDescritor chavePrimariaDescritor;\n"
                + "\n"
                + "    static {\n"
                + "        chavePrimariaDescritor = new ChavePrimariaDescritor();\n"
                + "        chavePrimariaDescritor.addCampo(\"cargoId\", Integer.class, \"cargo_id\", java.sql.Types.INTEGER);\n"
                + "        chavePrimariaDescritor.setAutogerada(true);\n"
                + "    }\n\n");
        modelo.append(criaSelect(tab));
        modelo.append(criaInsert(tab));
        modelo.append(criaUpdate(tab));
        modelo.append(criaDelete(tab));
        modelo.append(criaPreecheUpdate(tab));
        modelo.append(criaSetValues(tab));

        modelo.append("\n"
                + "    \n"
                + "    protected ChavePrimariaDescritor getChavePrimariaDescritor() {\n"
                + "        return chavePrimariaDescritor;\n"
                + "    }\n"
                + "\n"
                + "    \n"
                + "    protected ");
        modelo.append(tab.getNomeClasse());
        modelo.append(" newInstance() {\n"
                + "        return new ");
        modelo.append(tab.getNomeClasse());
        modelo.append("();\n"
                + "    }\n}");
        return modelo;
    }

    private static StringBuilder criaSelect(Tabela tab) {
        StringBuilder sql = new StringBuilder("SELECT ");
        String prefixo = tab.getNome().substring(0, 1).toUpperCase();

        boolean first = true;
        for (Coluna coluna : tab.getColunas()) {
            if (first) {
                first = false;
            } else {
                sql.append(",");
            }
            sql.append(prefixo);
            sql.append(".");
            sql.append(coluna.getNome());
        }

        sql.append(" FROM ");
        sql.append(tab.getNome());
        sql.append(" ");
        sql.append(prefixo);

        StringBuilder metodo = new StringBuilder("\n\t\n\tpublic String getSelect() {\n\t\treturn \"");
        metodo.append(sql);
        metodo.append("\";\n\t}\n");
        return metodo;
    }

    private static StringBuilder criaInsert(Tabela tab) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tab.getNome());
        sql.append(" (");

        boolean first = true;
        StringBuilder campos = new StringBuilder();
        for (Coluna coluna : tab.getColunas()) {
            if (first) {
                first = false;
            } else {
                sql.append(",");
                campos.append(",");
            }
            sql.append(coluna.getNome());
            campos.append("?");
        }
        sql.append(") VALUES (");
        sql.append(campos);
        sql.append(")");

        StringBuilder metodo = new StringBuilder("\n\t\n\tpublic String getInsert() {\n\t\treturn \"");
        metodo.append(sql);
        metodo.append("\";\n\t}\n");
        return metodo;
    }

    private static StringBuilder criaUpdate(Tabela tab) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tab.getNome());
        sql.append(" SET ");

        boolean first = true;
        for (Coluna coluna : tab.getColunas()) {
            if (first) {
                first = false;
            } else {
                sql.append(",");
            }
            sql.append(coluna.getNome());
            sql.append("=?");
        }

//        sql.append(" WHERE ID=?");


        StringBuilder metodo = new StringBuilder("\n\t\n\tpublic String getUpdate() {\n\t\treturn \"");
        metodo.append(sql);
        metodo.append("\";\n\t}\n");
        return metodo;
    }

    private static StringBuilder criaDelete(Tabela tab) {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(tab.getNome());
        sql.append(" WHERE ID=?");

        StringBuilder metodo = new StringBuilder("\n\t\n\tpublic String getDelete() {\n\t\treturn \"");
        metodo.append(sql);
        metodo.append("\";\n\t}\n");
        return metodo;
    }
    //protected void atribui(T entidade, ResultSet rs) throws DaoException {

    private static StringBuilder criaSetValues(Tabela tab) {
        StringBuilder sql = new StringBuilder("\n\n\t\n\tprotected void setValues(");
        String nomeVariavel = tab.getNomeClasse().substring(0, 1).toLowerCase();;
        sql.append(tab.getNomeClasse());
        sql.append(" ");
        sql.append(nomeVariavel);
        sql.append(", ResultSet rs) throws SQLException {");
        for (Coluna coluna : tab.getColunas()) {
            sql.append("\n\t\t");
            sql.append(nomeVariavel);
            sql.append(".set");
            sql.append(coluna.getNomeJavaParaMetodo());
            sql.append("(");
            if (coluna.isNullable()) {
                sql.append("get");
                sql.append(nomeTipoJdbc(coluna));
                sql.append("OrNull(rs,");
            } else {
                sql.append("rs.get");
                sql.append(nomeTipoJdbc(coluna));
                sql.append("(");
            }
            sql.append("\"");
            sql.append(coluna.getNome());
            sql.append("\"));");
        }
        sql.append("\n\t}\n");
        return sql;
    }

    private static StringBuilder criaPreecheUpdate(Tabela tab) {
        StringBuilder sql = new StringBuilder("\n\n\t\n\tprotected void prepareUpdate(PreparedStatement stmt, ");
        String nomeVariavel = tab.getNomeClasse().substring(0, 1).toLowerCase();
        sql.append(tab.getNomeClasse());
        sql.append(" ");
        sql.append(nomeVariavel);
        sql.append(") throws SQLException {");
        sql.append("\n\t\tint idx = 1;");

        for (Coluna coluna : tab.getColunas()) {
            if (coluna.isNullable()) {
                sql.append("\n\t\tsetNullSafe(stmt, ");
                sql.append(nomeVariavel);
                sql.append(".get");
                sql.append(coluna.getNomeJavaParaMetodo());
                sql.append("(), idx++);");
            } else {
                sql.append("\n\t\tstmt.set");
                sql.append(nomeTipoJdbc(coluna));
                sql.append("(idx++, ");
                if ("Timestamp".equals(nomeTipoJdbc(coluna))) {
                    sql.append("new java.sql.Timestamp(");
                    sql.append(nomeVariavel);
                    sql.append(".get");
                    sql.append(coluna.getNomeJavaParaMetodo());
                    sql.append("()");
                    sql.append(".getTime())");
                    sql.append(");");
                } else if ("Date".equals(nomeTipoJdbc(coluna))) {
                    sql.append("new java.sql.Date(");
                    sql.append(nomeVariavel);
                    sql.append(".get");
                    sql.append(coluna.getNomeJavaParaMetodo());
                    sql.append("()");
                    sql.append(".getTime())");
                    sql.append(");");
                } else if ("Time".equals(nomeTipoJdbc(coluna))) {
                    sql.append("new java.sql.Time(");
                    sql.append(nomeVariavel);
                    sql.append(".get");
                    sql.append(coluna.getNomeJavaParaMetodo());
                    sql.append("()");
                    sql.append(".getTime())");
                    sql.append(");");
                } else {
                    sql.append(nomeVariavel);
                    sql.append(".get");
                    sql.append(coluna.getNomeJavaParaMetodo());
                    sql.append("());");
                }
            }
        }
        sql.append("\n\t}\n");
        return sql;
    }

    private static String nomeTipoJdbc(Coluna coluna) {
        String tipou = coluna.getTipo().toUpperCase();
        if (tipou.contains("INT")) {
            return "Int";
        } else if (tipou.equals("LONG") || tipou.equals("NUMBER")) {
            return "Long";
        } else if (tipou.equals("BIT")) {
            return "Boolean";
        } else if (tipou.equals("DATE")) {
            return "Date";
        } else if (tipou.contains("BINARY") || tipou.equals("IMAGE")
                || tipou.startsWith("BLOB") || tipou.startsWith("MEDIUMBLOB")
                || tipou.startsWith("LONGBLOB") || tipou.equals("RAW")) {
            return "Blob";
        } else if (tipou.contains("DATETIME") || tipou.equals("TIMESTAMP") || tipou.equals("TIME")) {
            return "Timestamp";
        } else if (tipou.startsWith("DOUB") || tipou.startsWith("FLOA")
                || tipou.equals("MONEY") || tipou.equals("REAL")
                || tipou.equals("DECIMAL") || tipou.equals("NUMERIC")) {
            return "Double";
        } else if (tipou.startsWith("VARCH") || tipou.startsWith("CHA")
                || tipou.startsWith("NVARCH") || tipou.startsWith("TEXT")
                || tipou.startsWith("MEDIUMTEXT") || tipou.startsWith("LONGTEXT")) {
            return "String";
        } else if (tipou.equals("ENUM") || tipou.equals("SET")
                || tipou.equals("SYSNAME") || tipou.equals("NCHAR")
                || tipou.equals("XML")
                || tipou.equals("JSON")) {
            //return coluna.getNomeJavaParaMetodo()+"Tipo";
            return "String";
        }
        if(tipou.toLowerCase().startsWith("enum")) {
            return tipou;
        }
        throw new UnsupportedOperationException("Não implementamos a função para " + coluna.getTipo());
    }

    private static void configureConnection() {
        //-Dsgbd=mysql -Duser=root -Dhost=localhost -Ddatabase=autopeca_db -Dpassword=murilo
        //-Dsgbd=oracle -Duser=orami -Dpassword=me6a1nfra99 -Dhost=localhost -Ddatabase= -Dport1521:
//        System.setProperty("user", "sa");
//        System.setProperty("host", "192.168.0.150");
//        System.setProperty("schema", "dbo");
//        //System.setProperty("database", "autopeca");
//        System.setProperty("database", "adm");
//        System.setProperty("password", "trimpel");
    }

    private static void resultado(List<Tabela> tabelas) {
        exibeEmXml(tabelas);
        StringBuilder sb = new StringBuilder();
        if (false) {
            geraDDL(tabelas, sb);
        }

        geraSQL(tabelas, sb);
        List<StringBuilder> listaModelos = geraModelos(tabelas);
        List<StringBuilder> listaBuilders = geraModelosBuilder(tabelas);
        List<StringBuilder> listaModelosAtivos = geraModelosAtivos(tabelas);
        List<StringBuilder> listaDaos = geraDaos(tabelas);
        List<StringBuilder> listaJspEdits = geraJspEdits(tabelas);
//        List<StringBuilder> listaJspDetails = geraJspDetails(tabelas);
        List<StringBuilder> listaJspLists = geraJspLists(tabelas);
        List<StringBuilder> listaJspControllers = geraJspControllers(tabelas);
        

        File file = new File(BASEDIR);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }

        file = new File(BASEDIR + File.separator + "model");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
        
        file = new File(BASEDIR + File.separator + "builders");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
        
        file = new File(BASEDIR + File.separator + "modelActive");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }

        file = new File(BASEDIR + File.separator + "dao");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }

        file = new File(BASEDIR + File.separator + "jsp");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }

        file = new File(BASEDIR + File.separator + "controller");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
//        
//        file = new File(BASEDIR + File.separator + "dao");
//        if (!file.exists() || !file.isDirectory()) {
//            file.mkdir();
//        }

        try {
            String fileName = BASEDIR + File.separator + "ddl.sql";
            FileWriter fw = new FileWriter(fileName);
            fw.write(sb.toString());
            fw.close();

            for (int i = 0; i < tabelas.size(); i++) {
                fileName = BASEDIR + File.separator + "model" + File.separator
                        + tabelas.get(i).getNomeClasse();
                fileName += ".java";
                fw = new FileWriter(fileName);
                fw.write(listaModelos.get(i).toString());
                fw.close();
            }
            
            for (int i = 0; i < tabelas.size(); i++) {
                fileName = BASEDIR + File.separator + "builders" + File.separator
                        + tabelas.get(i).getNomeClasse();
                fileName += ".java";
                fw = new FileWriter(fileName);
                fw.write(listaBuilders.get(i).toString());
                fw.close();
            }

            for (int i = 0; i < tabelas.size(); i++) {
                fileName = BASEDIR + File.separator + "modelActive" + File.separator
                        + tabelas.get(i).getNomeClasse();
                fileName += "Model.java";
                fw = new FileWriter(fileName);
                fw.write(listaModelosAtivos.get(i).toString());
                fw.close();
            }

            for (int i = 0; i < tabelas.size(); i++) {
                fileName = BASEDIR + File.separator + "dao" + File.separator
                        + tabelas.get(i).getNomeClasse();
                fileName += "Dao.java";
                fw = new FileWriter(fileName);
                fw.write(listaDaos.get(i).toString());
                fw.close();
            }

            for (int i = 0; i < tabelas.size(); i++) {
                fileName = BASEDIR + File.separator + "controller" + File.separator
                        + tabelas.get(i).getNomeClasse();
                fileName += "Controller.java";
                fw = new FileWriter(fileName);
                fw.write(listaJspControllers.get(i).toString());
                fw.close();
            }

            Map<String, String> menuMap = new TreeMap<String, String>();
            for (int i = 0; i < tabelas.size(); i++) {
                String nvar = tabelas.get(i).getNomeVariavel();
                fileName = BASEDIR + File.separator + "jsp" + File.separator
                        + nvar + File.separator;
                fileName += "edit.jsp";
                menuMap.put(nvar + "Edit", fileName);
                file = new File(fileName);
                file = file.getParentFile();
                if (!file.exists()) {
                    file.mkdir();
                }

                fw = new FileWriter(fileName);
                fw.write(listaJspEdits.get(i).toString());
                fw.close();

//                fileName = BASEDIR + File.separator + "jsp" + File.separator
//                        + tabelas.get(i).getNomeClasse();
//                fileName += "detail.jsp";
//                fw = new FileWriter(fileName);
//                fw.write(listaJspDetails.get(i).toString());
//                fw.close();

                fileName = BASEDIR + File.separator + "jsp" + File.separator
                        + tabelas.get(i).getNomeVariavel() + File.separator;
                fileName += "list.jsp";
                menuMap.put(nvar + "List", fileName);
                fw = new FileWriter(fileName);
                fw.write(listaJspLists.get(i).toString());
                fw.close();
            }

            fileName = BASEDIR + File.separator + "jsp" + File.separator;
            fileName += "menu.jsp";
            fw = new FileWriter(fileName);
            fw.write(criaMenus(menuMap));
            fw.flush();
            fw.close();

            fileName = BASEDIR + File.separator + "jsp" + File.separator;
            fileName += "messages.properties";
            fw = new FileWriter(fileName);
            for (String msg : messages) {
                fw.write(msg);
                fw.write(" = ");
                fw.write(msg);
                fw.write("\n");
            }
            fw.flush();
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String criaMenus(Map<String, String> menuMap) {
        //StringBuilder sb = new StringBuilder("<menu type=\"list\">");
        StringBuilder sb = new StringBuilder("<nav>\n");
        for (String key : menuMap.keySet()) {
            messages.add(key);
            sb.append("<a href=\"");
            sb.append(menuMap.get(key));
            sb.append("\">");
            sb.append("<fmt:message key=\"").append(key).append("\"/>");
            sb.append("</a> |\n");
        }
        sb.append("</nav>\n");
        //sb.append("</menu>");

        return sb.toString();
    }

    private static List<StringBuilder> geraJspEdits(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<StringBuilder>();
        for (Tabela tab : tabelas) {
            sbs.add(geraJspEdit(tab));
        }
        return sbs;
    }

    private static StringBuilder geraJspEdit(Tabela tabela) {
        StringBuilder sb = new StringBuilder(
                "<%@ taglib uri=\"http://java.sun.com/jsp/jstl/fmt\" prefix=\"fmt\" %>\n"
                + "<%@ taglib uri=\"http://java.sun.com/jsp/jstl/core\" prefix=\"c\" %>\n"
                + "<%@page contentType=\"text/html\" pageEncoding=\"ISO-8859-1\"%>\n"
                + "<!DOCTYPE html><html><head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n");
        String nomeClasse = tabela.getNomeVariavel();
        nomeClasse = nomeClasse.substring(0, 1).toLowerCase() + nomeClasse.substring(1);
        String titulo = nomeClasse + "Edit";
        messages.add(nomeClasse);
        messages.add(titulo);
        sb.append("<title><fmt:message key=\"").append(titulo).append("\"/></title></head>\n<body>");
        sb.append("<h2><fmt:message key=\"").append(titulo).append("\"/></h2>\n");
        sb.append("<%@include file=\"/inc/menu.jsp\"%>\n");
        sb.append("<form name=\"").append(nomeClasse).append("\" action=\"").append(nomeClasse).append("/salvar\" method=\"POST\">\n<fieldset>\n");
        for (Coluna col : tabela.getColunas()) {
            sb.append(criaHtmlFormField(col));
        }
        sb.append("<input type=\"submit\" value=\"<fmt:message key=\"salvar\"/>\">\n");
        sb.append("</fieldset>\n</form>\n</body></html>");
        return sb;
    }

    private static String criaHtmlFormField(Coluna coluna) {
        String nome = coluna.getNomeJava();
        messages.add(nome);
        String field = "<label for=\"" + nome + "\"><fmt:message key=\"" + nome + "\"/></label>\n";
        //field += "<input type=\"text\" name="+nome+"  value="" disabled=\"disabled\"/>\n";
        field += "<input type=\"text\" name=\"" + nome + "\"";
        if (coluna.getTamanho() > 0) {
            field += " maxlength=\"" + coluna.getTamanho() + "\"";
        }
        if (!coluna.isNullable()) {
            field += " required=\"required\"";
        }
        field += "/>\n";
        return field;
    }

    private static List<StringBuilder> geraJspDetails(List<Tabela> tabelas) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static List<StringBuilder> geraJspLists(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<StringBuilder>();
        for (Tabela tab : tabelas) {
            sbs.add(geraJspList(tab));
        }
        return sbs;
    }

    private static StringBuilder geraJspList(Tabela tabela) {
        StringBuilder sb = new StringBuilder(
                "<%@ taglib uri=\"http://java.sun.com/jsp/jstl/core\" prefix=\"c\"%>\n"
                + "<%@ taglib uri=\"http://java.sun.com/jsp/jstl/fmt\" prefix=\"fmt\" %>\n"
                + "<%@page contentType=\"text/html\" pageEncoding=\"ISO-8859-1\"%>\n"
                + "<!DOCTYPE html><html><head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n");
        String nomeClasse = tabela.getNomeVariavel();
        nomeClasse = nomeClasse.substring(0, 1).toLowerCase() + nomeClasse.substring(1);
        String titulo = nomeClasse + "List";
        messages.add(nomeClasse);
        messages.add(titulo);
        sb.append("<title><fmt:message key=\"").append(titulo).append("\"/></title></head>\n<body>");
//        c
        sb.append("<h2><fmt:message key=\"").append(titulo).append("\"/></h2>\n");
        sb.append("<%@include file=\"/inc/menu.jsp\" %>\n");
//        <%@include file="/inc/menu.jsp" %>
        sb.append("<table class=\"ui-widget\" style=\"border-collapse: collapse;\">\n");
        sb.append("<thead class=\"ui-widget-header\">\n");
        sb.append(criaJspListTableHeader(tabela.getColunas()));
        //<tr><th>Prefixo</th><th>Nome</th><th></th></tr>
        sb.append("</thead>\n");
        sb.append("<tbody class=\"ui-widget-content\">\n");
        sb.append("<c:forEach var=\"it\" items=\"${requestScope.cooperadosListagem.cooperados}\">\n");
        sb.append(criaJspListTableBody(tabela.getColunas()));
        //<tr><td>${coop.prefixo}</td><td>${coop.nome}</td><td><a href="#" class="ui-state-default ui-corner-all" title="Visualizar cadastro"><span class="ui-icon ui-icon-search"></span></a></td></tr>
        sb.append("</c:forEach>");
        sb.append("</tbody></table>\n");
        sb.append("<%@include file=\"/inc/fotter.jsp\" %>\n");
        sb.append("</body></html>");
        return sb;
    }

    private static StringBuilder criaJspListTableHeader(List<Coluna> colunas) {
        StringBuilder sb = new StringBuilder("<tr>\n");
        for (Coluna col : colunas) {
            sb.append("<th>");
            sb.append("<fmt:message key=\"").append(col.getNomeJava()).append("\"/>");
            sb.append("</th>\n");
        }
        sb.append("</tr>\n");
        return sb;
    }

    private static StringBuilder criaJspListTableBody(List<Coluna> colunas) {
        StringBuilder sb = new StringBuilder("<tr>\n");
        for (Coluna col : colunas) {
            sb.append("<td>${it.");
            sb.append(col.getNomeJava());
            sb.append("}</td>\n");
        }
        sb.append("</tr>\n");
        return sb;
    }

    private static List<StringBuilder> geraJspControllers(List<Tabela> tabelas) {
        List<StringBuilder> sbs = new ArrayList<StringBuilder>();
        for (Tabela tab : tabelas) {
            sbs.add(geraJspController(tab));
        }
        return sbs;
    }

    private static StringBuilder geraJspController(Tabela tabela) {
        StringBuilder sb = new StringBuilder(
                "package controller;\n"
                + "\n"
                + "import br.com.jcomputacao.util.StringUtil;\n"
                + "import dao." + tabela.getNomeClasse() + "Dao;\n"
                + "import model." + tabela.getNomeClasse() + ";\n"
                + "import java.util.ArrayList;\n"
                + "import java.util.List;\n"
                + "import java.io.IOException;\n"
                + "import java.io.PrintWriter;\n"
                + "import javax.servlet.RequestDispatcher;\n"
                + "import javax.servlet.ServletException;\n"
                + "import javax.servlet.annotation.WebServlet;\n"
                + "import javax.servlet.http.HttpServlet;\n"
                + "import br.com.jcomputacao.util.web.HttpServletHelper;\n"
                + "import javax.servlet.http.HttpServletRequest;\n"
                + "import javax.servlet.http.HttpServletResponse;\n"
                + "\n"
                + "/**\n"
                + " *\n"
                + " * @author Murilo\n"
                + " */\n"
                + "@WebServlet(name = \"");
        sb.append(tabela.getNome());
        sb.append("Servlet\", urlPatterns = {\"/s/");
        sb.append(tabela.getNome());
        sb.append("\"})\n");
        sb.append("public class ");
        sb.append(tabela.getNomeClasse());
        sb.append("Controller extends HttpServletHelper  {\n");

        sb.append("\n"
                + "    private List<String> validate(");
        sb.append(tabela.getNomeClasse());
        sb.append(" ");
        sb.append(tabela.getNomeVariavel());
        sb.append(") {\n"
                + "        List<String> list = new ArrayList<String>();\n"
                + "        return list;\n"
                + "    }");

        sb.append("    /**\n"
                + "     * Processes requests for both HTTP\n"
                + "     * <code>GET</code> and\n"
                + "     * <code>POST</code> methods.\n"
                + "     *\n"
                + "     * @param request servlet request\n"
                + "     * @param response servlet response\n"
                + "     * @throws ServletException if a servlet-specific error occurs\n"
                + "     * @throws IOException if an I/O error occurs\n"
                + "     */\n"
                + "    protected void processRequest(HttpServletRequest request, HttpServletResponse response)\n"
                + "            throws ServletException, IOException {\n");

        sb.append("        StringBuilder msg = new StringBuilder();\n");

        StringBuilder setters = new StringBuilder();
        for (Coluna col : tabela.getColunas()) {
            sb.append("        String ");
            sb.append(col.getNomeJava());
            sb.append(" = request.getParameter(\"");
            sb.append(col.getNome());
            sb.append("\");\n");

            setters.append("        ");
            setters.append(tabela.getNomeVariavel());
            setters.append(".set");
            setters.append(col.getNomeJavaParaMetodo());
            setters.append("(");
            if ("String".equals(col.getTipoJava())) {
                setters.append(col.getNomeJava());
            } else if (null != col.getTipoEnum()) {
                setters.append(col.getTipoJava());
                setters.append(".valueOf(");
                setters.append(col.getNomeJava());
                setters.append(")");
            } else {
                setters.append("convertTo");
                setters.append(primeiraLetraMaiuscula(col.getTipoJava()));
                if (col.isNullable()) {
                    setters.append("OrNull(");
                } else {
                    setters.append("(");
                }
                setters.append(col.getNomeJava());
                setters.append(", request)");
            }

            setters.append(");\n");
        }

        sb.append("\n        ");
        sb.append(tabela.getNomeClasse());
        sb.append(" ");
        sb.append(tabela.getNomeVariavel());
        sb.append(" = null;\n");

        sb.append("\n");
        sb.append(setters);

        sb.append("        List<String> msgList = validate(");
        sb.append(tabela.getNomeVariavel());
        sb.append(");\n");

        sb.append("        if (msgList.isEmpty()) {\n");
        sb.append("            msg.append(\");");
        sb.append(tabela.getNomeVariavel());
        sb.append(" created with ID=\");\n");
        sb.append("            request.setAttribute(\"message\", msg);\n");
        sb.append("            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(\"/s/");
        sb.append(tabela.getNomeVariavel());
        sb.append("/list.jsp\");\n");
        sb.append("            dispatcher.forward(request, response);\n");
        sb.append("        } else {\n");
        sb.append("            for(String m:msgList) {\n");
        sb.append("                if(msg.length()>0) {\n");
        sb.append("                    msg.append(\"</br>\");\n");
        sb.append("                }\n");
        sb.append("                msg.append(m);\n");
        sb.append("            }\n");
        sb.append("            request.setAttribute(\"message\", msg.toString());\n");
        sb.append("            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(\"/s/");
        sb.append(tabela.getNomeVariavel());
        sb.append("/edit.jsp\");\n");
        sb.append("            dispatcher.forward(request, response);\n");
        sb.append("        }\n");
//                + "        String name = request.getParameter(\"name\");\n"
//                + "        String msg = null;\n"
//                + "        if (StringUtil.isNotNull(name)) {\n"
//                + "            Organization o = new Organization();\n"
//                + "            o.setName(name);\n"
//                + "            OrganizationDao od = new OrganizationDao();\n"
//                + "            od.persist(o);\n"
//                + "            msg = \"Organization created with ID=\" + o.getOrganizationId();\n"
//                + "            request.setAttribute(\"message\", msg);\n"
//                + "            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(\"/s/organization/list.jsp\");\n"
//                + "            dispatcher.forward(request, response);\n"
//                + "        } else {\n"
//                + "            msg = \"No name specified\";\n"
//                + "            request.setAttribute(\"message\", msg);\n"
//                + "            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(\"/s/organization/edit.jsp\");\n"
//                + "            dispatcher.forward(request, response);\n"
//                + "        }\n"
//                + "//        \n"
//                + "//        \n"
//                + "//        try {\n"
//                + "//            /* TODO output your page here. You may use following sample code. */\n"
//                + "//            out.println(\"<html>\");\n"
//                + "//            out.println(\"<head>\");\n"
//                + "//            out.println(\"<title>Servlet OrganizationSave</title>\");            \n"
//                + "//            out.println(\"</head>\");\n"
//                + "//            out.println(\"<body>\");\n"
//                + "//            out.println(\"<h1>\" + msg + \"</h1>\");\n"
//                + "//            out.println(\"<h1>\" + request.getContextPath() + \"</h1>\");\n"
//                + "//            out.println(\"</body>\");\n"
//                + "//            out.println(\"</html>\");\n"
//                + "//        } finally {            \n"
//                + "//            out.close();\n"
//                + "//        }\n"
        sb.append("    }\n");

        sb.append("\n    /**\n");
        sb.append("     * Handles the HTTP\n");
        sb.append("     * <code>POST</code> method.\n");
        sb.append("     *\n");
        sb.append("     * @param request servlet request\n");
        sb.append("     * @param response servlet response\n");
        sb.append("     * @throws ServletException if a servlet-specific error occurs\n");
        sb.append("     * @throws IOException if an I/O error occurs\n");
        sb.append("     */\n");
        sb.append("    \n");
        sb.append("    protected void doPost(HttpServletRequest request, HttpServletResponse response)\n");
        sb.append("            throws ServletException, IOException {\n");
        sb.append("        processRequest(request, response);\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    /**\n");
        sb.append("     * Returns a short description of the servlet.\n");
        sb.append("     *\n");
        sb.append("     * @return a String containing servlet description\n");
        sb.append("     */\n");
        sb.append("    \n");
        sb.append("    public String getServletInfo() {\n");
        sb.append("        return \"");
        sb.append(tabela.getNomeClasse());
        sb.append(" controller servlet\";\n");
        sb.append("    }// </editor-fold>\n");
        sb.append("}");
        return sb;
    }
    
    private static String primeiraLetraMaiuscula(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
