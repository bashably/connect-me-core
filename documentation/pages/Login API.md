- see [[Login Process]] for rules and more information to the underlying logic
- # Init/Reset Login Process (Step 0)
	- POST `/users/login/init`
	- No payload, just call
	- ### Responses
		- `200 OK` => Success
		- `403 FORBIDDEN` => You are not allowed to reset or init a login {{cloze maybe there is a failed or still pending verification attempt}}
	- ### Actions
		- reset the `StatefulLoginBean` in session if allowed
	- ### Patched Vulnerabilities
		- reset/re-init `StatefulLoginBean` in session when not allowed (e.g. while in [verification block](((62542000-13cc-4331-be2f-931eab1c896c))) ) => Check if reset allowed
		- reset/re-init `StatefulLoginBean` while pending verification attempt exists {{cloze would enable hackers to send infinite phone number verification SMS and harm us}} => Check if reset is allowed
- # Pass login data (Step 1)
	- POST `/users/login/userdata`
	- ### Payload
		- JSON of login data in content type `application/json`
		  ```json
		  {
		    "username": "...",
		    "passwordHash": "..."
		  }
		  ```
	- ### Responses
		- `200 OK` => login data was correct, step completed
		- `403 FORBIDDEN` => login data was not expected at current login state
		- `401 UNAUTHORIZED` => login data is not correct
	- ### Actions
		- check login data and store it in `StatefulLoginBean` for phone number verification
	- ### Patched Vulnerabilities
		- TODO payload too _heavy_ => Restrict content size of HTTP request
		- Secured against Injection (no logging of user data and JPA backend)
- # Start phone number verification (Step 2)
	- POST `/users/login/verify/start`
	- No payload, just call
	- ### Responses
		- `200 OK` => phone number verification process started and code sent via SMS
		- `403 FORBIDDEN` => this interaction was not expected in the current login state OR another verification attempt is currently not allowed
	- ### Actions
		- check if another phone number verification attempt is currently allowed
		- generate verification code
		- send verification code via SMS to users phone number
	- ### Patched Vulnerabilites
		- hacker requests too many SMS in order to harm us financially => limit phone number verification attempts and check if another is allowed before sending SMS
- # Pass verification code (Step 3)
	- POST `/users/login/verify/check`
	- ### Payload
		- the verification code in content type `text/plain`
	- ### Responses
		- `200 OK` => phone number verification completed and user successfully logged in. This response contains the **JWT** that authenticates the user in `text/plain` format.
		- `403 FORBIDDEN` => this interaction was not expected in the current login state
		- `400 BAD REQUEST` => the passed verification code is not correct. A new phone number verification must be requested (Step 2)
	- ### Actions
		- check if the passed verification code is correct
		- finalize login
			- generate JWT token for user (automatically logs out other users by invalidating their JWT)
			- grant users with JWT access to user interactions
		- Return response containing JWT
	- ### Patched Vulnerabilities
		- TODO payload too _heavy_ => Restrict content size of HTTP request