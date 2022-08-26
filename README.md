............................. RC CUBE (For Neutron Star @FEB 16, 2001) ..............................

# A modular 3D N*N rubik's cube program with solver and command line control

# Usage

1. Install Java on your computer and add it to the path
2. Copy folder "out\artifacts\cube_jar" to your computer
2. click "launch.bat"

# Moves (Clockwise)

1. U: Up
2. R: Right
3. F: Front
4. D: Down
5. L: Left
6. B: Back

7. Moves are case insensitive
8. Add prime (') for anticlockwise move. ex: R (for clockwise) -> R' (for anti-clockwise)
9. type move twice for a half turn (180 deg). ex: R (for single turn) -> RR (for double turn)
10. To turn middle slice, add it's index [0, n-1] from the side of move. ex: R1 (turns 2nd slice from right),  U'3 (turns 4th slice from UP anticlockwise])
11. To turn multiple slices in a single move, type their indices in brackets separated by comma. ex: R[0,2] (turns 1st and 3rd slices from right in a 5*5 cube)


# Commands

1. n [dimension] -> set cube dimension
2. scramble [moves]: scramble with given no of moves
3. solve: Solve/Apply solution (Only for 3*3 cube)
4. reset [what]: Reset [cube, zoom]
5. undo: undo last move
6. finish [c]: finish/cancel animating and pending moves
7. enter moves sequence (moves separated by space)
8. exit/quit: quit

# Demo

https://www.youtube.com/watch?v=Ivm3o5bG-fI
