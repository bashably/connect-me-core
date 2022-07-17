package org.connectme.core.interests.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name="interest_term")
public class InterestTerm {

    @Id @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = Interest.class)
    @JoinColumn(name = "interest_id")
    @JsonIgnore
    private Interest root;

    @Column(name="lang")
    @JsonProperty("lang")
    private String languageCode;

    @Column(name="term")
    @JsonProperty("term")
    private String term;

    public InterestTerm() {}

    public InterestTerm(Interest rootInterest, String term, String lang) {
        this.term = term;
        this.languageCode = lang;
        this.root = rootInterest;
    }


    public Long getId() {
        return id;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getTerm() {
        return term;
    }

    public Interest getRoot() {
        return root;
    }

    public void setRoot(Interest _root) {
        this.root = _root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterestTerm that = (InterestTerm) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getLanguageCode(), that.getLanguageCode()) && Objects.equals(getTerm(), that.getTerm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getLanguageCode(), getTerm());
    }
}
