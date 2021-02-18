/*
 *
 *
 */

package com.distsys.dbbuilder;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author David Ohio
 */
@XStreamAlias("Tabela")
public class Tabela {
    @XStreamAlias("nome")
    private String nome;
    private String nomeClasse;
    @XStreamImplicit(itemFieldName = "coluna")
    private List<Coluna> colunas;
    @XStreamOmitField
    private Object[] valores;

    public List<Coluna> getColunas() {
        return colunas;
    }

    public void setColunas(List<Coluna> colunas) {
        this.colunas = colunas;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        if(this.nomeClasse==null) {
            this.nomeClasse = this.nome.replaceAll("_", "");
        }
    }

    void setNomeClasse(String nomeClasse) {
        this.nomeClasse = nomeClasse;
    }
    
    public String getNomeClasse() {
        return this.nomeClasse;
    }
    
    public String getNomeVariavel() {
        return this.nomeClasse.substring(0, 1).toLowerCase() + this.nomeClasse.substring(1);
    }

    public boolean addColuna(String coluna){
        return addColuna(new Coluna(coluna));
    }

    public boolean addColuna(Coluna coluna){
        if (colunas == null){
            colunas = new ArrayList<>();
        }
        return colunas.add(coluna);
    }

    public Object[] getValores() {
        return valores;
    }

    public void setValores(Object[] valores) {
        this.valores = valores;
    }
}
