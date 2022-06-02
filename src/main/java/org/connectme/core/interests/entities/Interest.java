package org.connectme.core.interests.entities;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This entity contains all data of an interest (incl. different terms in different languages).
 * @author Daniel Mehlber
 */
@Table(name="interest_root")
@Entity
public class Interest {

    @Id @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_on")
    @CreationTimestamp
    private LocalDateTime createdOn;

    @Column(name = "last_update_on")
    @UpdateTimestamp
    private LocalDateTime updatedOn;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "root", cascade = CascadeType.ALL, targetEntity = InterestTerm.class)
    private Set<InterestTerm> terms;

    public Interest() {}

    public Long getId() {
        return id;
    }

    public Set<InterestTerm> getTerms() {
        return terms;
    }

    public void setTerms(Set<InterestTerm> terms) {
        // this is a bidirectional relationship, so set the new parent
        for(InterestTerm term : terms) {
            term.setRoot(this);
        }
        this.terms = terms;
    }

    public void setTerms(InterestTerm... terms) {
        setTerms(new HashSet<>(Arrays.asList(terms)));
    }
}
