/*
 *
 *
 */

package com.distsys.dbbuilder;

/**
 * 08/10/2010 22:13:56
 * @author Murilo
 */
public class Coluna {
    private final String nome;
    private String tipo;
    private String[] tipoEnum = null;
    private int tamanho;
    private String tipoJava;
    private String nomeJava;
    private boolean nullable;

    public Coluna(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public int getTamanho() {
        return tamanho;
    }

    public void setTamanho(int tamanho) {
        this.tamanho = tamanho;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipoJava() {
        return tipoJava;
    }

    public void setTipoJava(String tipoJava) {
        this.tipoJava = tipoJava;
    }

    public void setNomeJava(String nomeJava) {
        this.nomeJava = nomeJava;
    }

    public String getNomeJava() {
        return nomeJava;
    }

    public String getNomeJavaParaMetodo() {
        return nomeJava.substring(0, 1).toUpperCase()+nomeJava.substring(1);
    }

    public String[] getTipoEnum() {
        return tipoEnum;
    }

    public void setTipoEnum(String[] tipoEnum) {
        this.tipoEnum = tipoEnum;
    }

    boolean isNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }
}
