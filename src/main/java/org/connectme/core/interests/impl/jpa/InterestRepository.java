package org.connectme.core.interests.impl.jpa;

import org.connectme.core.interests.entities.Interest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestRepository extends CrudRepository<Interest, Long> {

}
