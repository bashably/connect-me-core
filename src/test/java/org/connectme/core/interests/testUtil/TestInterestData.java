package org.connectme.core.interests.testUtil;

import org.connectme.core.interests.entities.Interest;
import org.connectme.core.interests.entities.InterestTerm;
import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TestInterestData {

    private static List<Interest> interests = new LinkedList<>();


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


    public static Interest getRandomInterest(InterestRepository repository) {
        int index = new Random().nextInt((int) repository.count());
        Interest interest = null;
        int i = 0;
        for(Interest _interest : repository.findAll()) {
            if(i == index) {
                interest = _interest;
                break;
            }
            i++;
        }

        return interest;
    }


    public static InterestTerm getRandomInterestTerm(InterestTermRepository repository) {
        int index = new Random().nextInt((int) repository.count());
        InterestTerm interestTerm = null;
        int i = 0;
        for(InterestTerm _interestTerm : repository.findAll()) {
            if(i == index) {
                interestTerm = _interestTerm;
                break;
            }
            i++;
        }

        return interestTerm;
    }

    public static void fillRepository(InterestRepository interestRepository) {
        interestRepository.saveAll(interests);
    }
}
