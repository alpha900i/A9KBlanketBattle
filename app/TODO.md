# TODO
## PRIMARY
## SECONDARY
1. "Selected piece type" better visuals
2. "Game over" visuals
3. Current "hand.changed" methods makes no sense - it should be "hand changed from starting", not "there is no pieces on board"
4. Should methods in Game be static?
## TERTIARY
1. Readme
2. ColorScheme & typography
3. contentDescriptions for images
4. Better UI for Start Screen
5. Documentation/comments
6. Lint
7. Extract dimens && magic numbers (delays)
8. More defensives 
9. winnerIndex is code-smelly - field, that doesn't make sense a lot of times... 
10. owner for empty cells is code-smelly. Maybe sealed class?
## QUESTIONS
1. Board (and maybe someone else) kinda does a lot with incoming params, isn't it? It uses them as return value. Not pure.
2. Bot3 - something clever?