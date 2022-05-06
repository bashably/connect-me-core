title:: Process Implementation using Beans

- The user management relies on processes like the [[Registration Process]] with multiple steps (e.g. collecting data, verification steps, etc) that the client/user can interact with.
- In order to keep the state and required/collected data of a running process, an object will be stored in the users session => The **Stateful session bean**. It represents the running process
- [[draws/2022-04-17-01-58-20.excalidraw]]
- 1) The client talks to the Rest API and request an interaction
- 2) A Stateful Bean (=State-Machine) is embedded in the clients session. It gets accessed by the Rest API.
	- 2a) Inside the _Stateful Bean_ the requested interaction gets checked with the beans state (often interactions have to commence in a specific order e.g. first input user data, then verify).
	- 2b) If the interaction is expected/allowed it will be executed and the _Stateful Beans_ state changes.
- 3) The user continues to work with the Stateful Bean (placed in his session) until he reaches the final step
- 4) After the final step has been completed, the bean will talk to the logic in order to store the collected and verified data in the database or to execute further actions.
- 5) The Bean will be destroyed