- # Types of Tests
	- There are multiple types of tests. Mainly we are using
	- ## Unit Tests
		- Unit tests are tests for single (small) units of code
		- **Example**: There is a method that tests if an entered username is allowed or not. This method is tested using unit testing, ^^because is requires no connection to external systems^^ {{cloze such as databases or other APIs}}
		- ```java
		  public class UsernameCheckTest {
		    
		    // list of possible allowed usernames
		    private String[] allowedUsernames = {
		      "mb_nator", "username123", "hello.world", ...
		    };
		    
		    // list of possible forbidden usernames
		    private String[] forbiddenUsernames = {
		      ".", "", "!?!", "<script>alert(1)</script>", ...
		    };
		    
		    @Test
		    public void testAllowed() {
		      for(String allowed : allowedUsernames) {
		        Assertions.assertTrue(UsernameChecker.isAllowed(allowed));
		      }
		    }
		    
		    @Test
		    public void testForbidden() {
		      for(String forbidden : forbiddenUsernames) {
		        Assertions.assertFalse(UsernameChecker.isAllowed(allowed));
		      }
		    }
		  }
		  ```
		- The tests above will be run in **random order** and will ensure that the `UsernameChecker.isAllowed(final String username)` method works as expected.
		- Tests can be run using `mvn test` command
	- ## Integration Tests (+API tests)
		- Integration tests are for compositions of components that require connections to other systems in order to function.
		- **Example**: There is a registration process consisting of
			- REST API
			- Process Logic / Business Logic
			- Database entities
		- This process is tested using integration testing because ^^the system has to be tested inside an environment of other systems^^. Process-tests are often integration tests because they require a REST-API and database.
		- Frankly, Spring Boot and Docker allow us to test our REST-APIs and integrate our databases dynamically.
		- ```java
		  @SpringBootTest
		  @AutoConfigureMockMvc
		  class ApplicationTests {
		  
		      @Autowired
		      private MockMvc client;
		  
		      @Autowired
		      private PersonLogic personLogic;
		  
		      @Test
		      public void testKnownPersonFetch() throws Exception {
		          // prepare
		          Person person = new Person("Daniel", "Mehlber", 20, "I love programming");
		          personLogic.addPerson(person);
		  
		          // validate
		          client.perform(get(String.format("/person/id/%d", person.getId())))
		                  .andExpect(status().isOk())
		                  .andExpect(content().json(new ObjectMapper().writeValueAsString(person)));
		      }
		  }
		  ```
		- In order to run this test we need to fire up the systems required environment.
		- ```shell
		  # 1) start databases or other required systems using docker compose
		  docker-compose -f docker-compose-environment.yml build
		  docker-compose -f docker-compose-environment.yml up -d
		  
		  # 2) run tests
		  mvn test
		  
		  # 3) clean up databases or other systems
		  docker-compose down
		  ```
		- This requires a `docker-compose-environment.yml` script that only established all required systems for testing purposes. It resides in the root directory of the project.
		- ## Mock sessions
			- In order to use sessions we have to use the `MockHttpSession` class. The code shown below tests a transactions on the same session:
			- ```java
			  @Test
			  public void happyPath() throws Exception {
			    // create mock session (must be passed in client request)
			    MockHttpSession session = new MockHttpSession();
			  
			    // 1) init registration
			    client.perform(post("/users/registration/init").session(session)).andExpect(status().isOk());
			  
			    // 2) send user registration data
			    final RegistrationUserData userData = UserDataRepository.assembleValidRegistrationUserData();
			    String json = new ObjectMapper().writeValueAsString(userData);
			  
			    client.perform(post("/users/registration/set/userdata")
			                   .contentType("application/json")
			                   .content(json)
			                   .session(session))
			      .andExpect(status().isOk());
			  
			    // 3) start verification process
			    client.perform(post("/users/registration/start/verify")
			                   .session(session))
			      .andExpect(status().isOk());
			  
			    // 4) pass verification code
			    StatefulRegistrationBean registrationObject = (StatefulRegistrationBean) session.getAttribute(RegistrationAPI.SESSION_REGISTRATION);
			    String code = registrationObject.getVerificationCode();
			  
			    client.perform(post("/users/registration/verify")
			                   .contentType("text/plain")
			                   .content(code)
			                   .session(session))
			      .andExpect(status().isOk());
			  }
			  ```