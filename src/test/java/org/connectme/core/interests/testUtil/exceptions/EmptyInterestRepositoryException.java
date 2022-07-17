package org.connectme.core.interests.testUtil.exceptions;

import org.connectme.core.interests.impl.jpa.InterestRepository;

/**
 * no interests or interest terms were found in interest repository. In order to retrieve test data, the repository
 * must be filled with test interest(-terms). In order to do so, use
 * {@link org.connectme.core.interests.testUtil.InterestRepositoryTestUtil#fillRepositoryWithTestInterests(InterestRepository)}
 * before your unit test in order to provide a set of test interest(-terms) the unit tests can work with.
 */
public class EmptyInterestRepositoryException extends RuntimeException {

    public EmptyInterestRepositoryException() {
        super("not enough or no interest(-terms) were found in provided interest repository. You may be forgotten to fill" +
                " the repository before the unit test");
    }

}
