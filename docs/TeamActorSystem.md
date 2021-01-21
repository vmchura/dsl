TeamSystem
- TeamCreator: Create a team
- TeamDestroyer: Destroy a team
- TeamManager: Invite and accept members for a team
- MemberSupervisor: Position of members 

TeamCreator
- A user which is not official creates a team with a name, also as principal
- Team manager add this user as official

TeamDestroyer
- Waits until all members are free then destroys the team

TeamManager
- Principal can send an invitation to user if applicable
- User can accept or deny an invitation
- Remove user from a team
- Add user to a team

MemberSupervisor
- Check if a member is official of any team
- Check if a member is principal for a specific team

InvitationManager
- Receives an invitation request for a user in a specific position in team
- Receives an invitation response, accepting it or denying it.
    - Handles if an invitation is still coherent if accepted.
