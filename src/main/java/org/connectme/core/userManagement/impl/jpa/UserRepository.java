package org.connectme.core.userManagement.impl.jpa;

import org.connectme.core.userManagement.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface UserRepository extends CrudRepository<User, String> {

    // will be automatically implemented by spring boot
    boolean existsByPhoneNumber(final String phoneNumber);

}
