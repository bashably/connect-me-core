package org.connectme.core.interests.impl.jpa;

import org.connectme.core.interests.entities.InterestTerm;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestTermRepository extends CrudRepository<InterestTerm, Long> {
    List<InterestTerm> searchByTerm(final String term);

    List<InterestTerm> getByRootId(final Long rootId);

    @Query("SELECT term FROM InterestTerm term WHERE interest_id = :rootId AND lang = :langCode")
    List<InterestTerm> getByRootIdInLanguage(@Param("rootId") final Long rootId, @Param("langCode") final String langCode);
}
