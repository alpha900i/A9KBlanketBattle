# TODO
## PRIMARY
## SECONDARY
## TERTIARY
1. Lint
2. More defensives 
3. Tests - actively use assertCellEmpty/assertCellPiece
4. winnerIndex in MoveResult deserve additional treatment. Maybe not sealed class, but...
5. Remove destructuring from MoveResult process, it became to big for its own good
6. maxPieceAmount is constant throughout the game. Move to ... somewhere?
7. Triplet naming on deletion may use direction factor - otherwise "same starts" have same name
## QUESTIONS
1. Board (and maybe someone else) kinda does a lot with incoming params, isn't it? It uses them as return value. Not pure.
2. Bot3 - something clever?