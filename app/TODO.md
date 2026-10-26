# TODO
## PRIMARY
## SECONDARY
1. "Selected piece type" better visuals
2. "Game over" visuals
3. Current "hand.changed" methods makes no sense - it should be "hand changed from starting", not "there is no pieces on board"
## TERTIARY
1. ColorScheme & typography
2. contentDescriptions for images
3. Better UI for Start Screen
4. Documentation/comments
5. Lint
6. Extract dimens && magic numbers (delays)
7. More defensives 
8. winnerIndex is code-smelly - field, that doesn't make sense a lot of times... 
9. owner for empty cells is code-smelly. Maybe sealed class?
## QUESTIONS
1. Board (and maybe someone else) kinda does a lot with incoming params, isn't it? It uses them as return value. Not pure.
2. Bot3 - something clever?