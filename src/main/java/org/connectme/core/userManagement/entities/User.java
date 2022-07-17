package org.connectme.core.userManagement.entities;

import org.connectme.core.interests.entities.InterestTerm;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.geo.Point;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * user data as it is stored in the database
 */
@Entity
public class User {

    @Id @Column(name = "username")
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "auth_token")
    private String authToken;

    @Column(name = "current_location")
    private Point currentLocation;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable( name = "user_interest_term",
                joinColumns = @JoinColumn(name="user_id", referencedColumnName = "username"),
                inverseJoinColumns = @JoinColumn(name = "interest_term_id", referencedColumnName = "id"))
    private Set<InterestTerm> interestTerms;

    @Column(name = "CREATED_ON")
    @CreationTimestamp
    private LocalDateTime createdOn;

    @Column(name = "LAST_UPDATE_ON")
    @UpdateTimestamp
    private LocalDateTime updatedOn;

    public User() {}

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Point getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Point currentLocation) {
        this.currentLocation = currentLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(getUsername(), user.getUsername()) && Objects.equals(getPasswordHash(), user.getPasswordHash());
    }

    public Set<InterestTerm> getInterestTerms() {
        return interestTerms;
    }

    public void setInterestTerms(final Set<InterestTerm> terms) {
        interestTerms = terms;
    }

    public void addInterestTerm(final InterestTerm term) {
        interestTerms.add(term);
    }

    public void removeInterestTerm(final InterestTerm term) {
        interestTerms.remove(term);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
