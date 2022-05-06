- In order to authorize the execution of a request and authentication has to be successful.
- For Connect Me it is important that **there is only 1 active client per user**. Anything else would counteract the phone number verification. This alters the authentication design because we cannot just use session based authentication => application scope authentication is required.
- # How it works
	- The Authentication is handled by the application scope and singleton `UserAuthenticationBean`
	- The Authentication is working with **JWT tokens** and custom authentication tokens.
	- The JWT contains
		- `username` claim => identifies the user ^^who is sending the request^^
		- `authToken` => acts like an **access key**. There is only 1 valid `authToken` for each user at a time {{cloze because only 1 device is allowed for each user at a time}} . At login, each user gets assigned a new `authToken` {{cloze rendering the old token obsolete and invalid}}. This means that the old client {{cloze e.g. when switching to another device}} looses access with his _old_ token => **When a new client logs in, the other client gets logged out**. This ensures that each ^^user has at maximum 1 active client/device^^ and is the main reason for the `authToken` claim.
	- The user can only be authorized if
		- 1. the JWT Token is valid
		  2. the JWT contains an existing username as claim
		  3. the JWT contains the exact `authToken` associated to the `username`
- # Steps when a request arrives
	- 1) Receive request containing JWT Token
	- 2) Process JWT Token
		- 1) Verify JWT Token using its secret (standard action when working with JWT)
		- 2) Extract `username` and `authToken` claims
	- 3) Process Claims
		- 1) Fetch user data associated with `username`
		- 2) Make sure the actual `authToken` matches the claimed `authToken`