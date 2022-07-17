package org.connectme.core.userManagement.testUtil;

import org.connectme.core.interests.impl.jpa.InterestRepository;
import org.connectme.core.interests.impl.jpa.InterestTermRepository;
import org.connectme.core.interests.testUtil.InterestRepositoryTestUtil;
import org.connectme.core.userManagement.entities.PassedUserData;

import java.util.Random;

/**
 * This util class contains a repository of usernames and password (both allowed and forbidden) that can be used
 * by various test scenarios.
 */

public class UserRepositoryTestUtil {


    /**
     * Repository of usernames (both allowed and forbidden) for various testing scenarios
     */
    public static class Usernames {

        /**
         * list of allowed usernames
         */
        public static final String[] allowed = {
                "mb_nator",
                "username123",
                "davewasalreadytaken",
                "the_unserscore_user"
        };

        /**
         * list of forbidden usernames
         */
        public static final String[] forbidden = {
                "white space",
                "multi\nline",
                "tabs\ttabs",
                "excamation!",
                "question?",
                "point.syntax",
                "list,list,list",
                "</hacker>",
        };


        /**
         * selects a random allowed username from list and returns it
         * @return a random username that is allowed by the system
         */
        public static String getRandomAllowed() {
            int randomIndex = new Random().nextInt(allowed.length);
            return allowed[randomIndex];
        }

        /**
         * selects a random forbidden username from list and returns it
         * @return a random username that is forbidden by the system
         */
        public static String getRandomForbidden() {
            int randomIndex = new Random().nextInt(forbidden.length);
            return forbidden[randomIndex];
        }

    }

    public static class Passwords {

        public static final String[] allowed = {
                "kajfhlaksjfh394857345h3kjh",
                "s8d7f6s8d7f6s8d7fs8d7f",
                "wrl645pha"
        };

        public static final String[] forbidden = {
                "hallo",
                "1234",
                "password3"
        };

        public static String getRandomAllowed() {
            int randomIndex = new Random().nextInt(allowed.length);
            return allowed[randomIndex];
        }

        public static String getRandomForbidden() {
            int randomIndex = new Random().nextInt(forbidden.length);
            return forbidden[randomIndex];
        }
    }

    public static class PhoneNumbers {

        /**
         * list of allowed phone numbers
         */
        public static final String[] allowed = {
                "0 0000 000000"
        };

        /**
         * list of forbidden phone numbers
         */
        public static final String[] forbidden = {
                "noNumber"
        };


        /**
         * selects a random allowed phone number from list and returns it
         * @return a random phone number that is allowed by the system
         */
        public static String getRandomAllowed() {
            int randomIndex = new Random().nextInt(allowed.length);
            return allowed[randomIndex];
        }

        /**
         * selects a random forbidden phone number value from list and returns it
         * @return a random phone number that is forbidden by the system
         */
        public static String getRandomForbidden() {
            int randomIndex = new Random().nextInt(forbidden.length);
            return forbidden[randomIndex];
        }
    }

    /**
     * Assembles instance of {@link PassedUserData} containing valid user data.
     * <h2>Important</h2>
     * This method requires a filled interest term repository due to the required Set of interest term ids. It can be filled using
     * {@link InterestRepositoryTestUtil#fillRepositoryWithTestInterests(InterestRepository)}.
     * @param interestTermRepository interest term repository used for generating a set of interest term ids passed as user data.
     * @return valid user data of instance {@link PassedUserData}
     * @author Daniel Mehlber
     */
    public static PassedUserData assembleValidPassedUserData(InterestTermRepository interestTermRepository) {
        return new PassedUserData(
                UserRepositoryTestUtil.Usernames.getRandomAllowed(),
                UserRepositoryTestUtil.Passwords.getRandomAllowed(),
                UserRepositoryTestUtil.PhoneNumbers.getRandomAllowed(),
                InterestRepositoryTestUtil.getRandomInterestTermIds(interestTermRepository, 5));
    }

    public static PassedUserData assembleForbiddenPassedUserData(InterestTermRepository interestTermRepository) {
        return new PassedUserData(
                UserRepositoryTestUtil.Usernames.getRandomForbidden(),
                UserRepositoryTestUtil.Passwords.getRandomForbidden(),
                UserRepositoryTestUtil.PhoneNumbers.getRandomForbidden(),
                InterestRepositoryTestUtil.getRandomInterestTermIds(interestTermRepository, 5));
    }

}
