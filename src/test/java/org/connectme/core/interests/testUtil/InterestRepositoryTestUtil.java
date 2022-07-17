package org.connectme.core.interests.testUtil;

import org.connectme.core.interests.entities.Interest;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.exceptions.EmptyInterestRepositoryException;

import java.util.*;
import java.util.stream.Collectors;


public class InterestRepositoryTestUtil {

    private static final List<Interest> interests = new LinkedList<>();


    static {
        // programming
        Interest programming = new Interest();
        programming.setTerms(
                new InterestTerm(programming, "programming", "en"),
                new InterestTerm(programming, "programmieren", "de"),
                new InterestTerm(programming, "code", "en"),
                new InterestTerm(programming, "编程", "ch")
        );
        interests.add(programming);

        // party
        Interest party = new Interest();
        party.setTerms(
                new InterestTerm(party, "party", "en"),
                new InterestTerm(party, "feiern", "de"),
                new InterestTerm(party, "saufen", "de"),
                new InterestTerm(party, "庆祝", "ch")
        );
        interests.add(party);

        // drawing
        Interest drawing = new Interest();
        drawing.setTerms(
                new InterestTerm(drawing, "drawing", "en"),
                new InterestTerm(drawing, "zeichnen", "de")
        );
        interests.add(drawing);
    }

    public static List<Interest> getInterests() {
        return interests;
    }


    public static Interest getRandomInterest(InterestRepository interestRepository) {
        // check if there is at least 1 interest in this repository
        if(interestRepository.count() < 1)
            throw new EmptyInterestRepositoryException();

        int index = new Random().nextInt((int) interestRepository.count());
        Interest interest = null;
        int i = 0;
        for(Interest _interest : interestRepository.findAll()) {
            if(i == index) {
                interest = _interest;
                break;
            }
            i++;
        }

        return interest;
    }


    public static InterestTerm getRandomInterestTerm(InterestTermRepository interestTermRepository) {
        // check if there is at least 1 interest term in this repository
        if(interestTermRepository.count() < 1)
            throw new EmptyInterestRepositoryException();

        int index = new Random().nextInt((int) interestTermRepository.count());
        InterestTerm interestTerm = null;
        int i = 0;
        for(InterestTerm _interestTerm : interestTermRepository.findAll()) {
            if(i == index) {
                interestTerm = _interestTerm;
                break;
            }
            i++;
        }

        return interestTerm;
    }

    public static void fillRepositoryWithTestInterests(InterestRepository interestRepository) {
        interestRepository.saveAll(interests);
    }

    public static void clearRepository(InterestRepository interestRepository) {
        interestRepository.deleteAll();
    }

    public static Set<InterestTerm> getRandomInterestTerms(InterestTermRepository interestTermRepository, int amount) {
        // check if there are at least 5 interest terms in this repository
        if(interestTermRepository.count() < 5)
            throw new EmptyInterestRepositoryException();

        Set<InterestTerm> interestTerms = new HashSet<>();
        do  {
            interestTerms.add(getRandomInterestTerm(interestTermRepository));
        } while (interestTerms.size() < amount);

        return interestTerms;
    }

    public static Set<Long> getRandomInterestTermIds(InterestTermRepository interestTermRepository, int amount) {
        return getRandomInterestTerms(interestTermRepository, amount).stream().map(InterestTerm::getId).collect(Collectors.toSet());
    }
}
