def dice(image):
    board = [[0 for _ in range(len(image[0]))] for _ in range(len(image))]
    for i in range(len(image)):
        for j in range(len(image[i])):
            if image[i][j] == 1:
                continue

            for k in range(-1, 2):
                for l in range(-1, 2):
                    if k == 0 and l == 0:
                        continue
                    if i + k < 0 or j + l < 0 or i + k >= len(image) or j + l >= len(image[i]):
                        continue

                    board[i][j] += image[i + k][j + l]

            if board[i][j] > 6:
                board[i][j] = 0

    return board


def main():
    with open(sys.argv[1], 'r') as file:
        rows = [[int(c) for c in line.strip()] for line in file]

    size = len(rows)
    image = [[0] * size for _ in range(size)]
    for i, row in enumerate(rows):
        image[i] = row
        size = max(size, len(row))

    board = dice(image)
    with open(sys.argv[2], 'w') as file:
        for row in board:
            file.write(''.join(map(str, row)) + '\n')


if __name__ == "__main__":
    import sys
    main()
