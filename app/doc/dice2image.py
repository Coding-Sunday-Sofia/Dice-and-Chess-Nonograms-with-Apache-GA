import sys
import numpy as np
from PIL import Image
from PIL import ImageEnhance

# Load the text file containing the matrix
def load_matrix_from_file(filename):
    with open(filename, 'r') as file:
        matrix = [list(line.strip()) for line in file]
    return np.array(matrix)

# Define a mapping between numbers and die face images
die_faces = {
    '0': '0.png',
    '1': '1.png',
    '2': '2.png',
    '3': '3.png',
    '4': '4.png',
    '5': '5.png',
    '6': '6.png'
}

# Load die face images
def load_die_faces():
    return {num: Image.open(filename) for num, filename in die_faces.items()}

# Assemble the images into a final image with color shift
def assemble_images(matrix, die_faces, color_shift_factor=0.25):
    face_size = die_faces['0'].size  # Assuming all face images have the same size
    width, height = matrix.shape[1] * face_size[0], matrix.shape[0] * face_size[1]
    final_image = Image.new('RGB', (width, height), color='white')

    # Define the color shift for grayscale images
    enhancer = ImageEnhance.Brightness

    for y, row in enumerate(matrix):
        for x, num in enumerate(row):
            if num in die_faces:
                # Load the die face image
                die_face_image = die_faces[num]

                # Apply color shift to the grayscale representation of the image
                gray_image = die_face_image.convert('L')
                enhanced_image = enhancer(gray_image).enhance(1 + color_shift_factor)

                # Paste the color-shifted image onto the final image
                final_image.paste(enhanced_image, (x * face_size[0], y * face_size[1]))

    return final_image

# Save the final image
def save_image(image, filename):
    image.save(filename)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python dice2image.py input_text_file output_image_file")
        sys.exit(1)

    input_text_file = sys.argv[1]
    output_image_file = sys.argv[2]

    matrix = load_matrix_from_file(input_text_file)

    die_faces = load_die_faces()

    final_image = assemble_images(matrix, die_faces, color_shift_factor=0.05)

    save_image(final_image, output_image_file)
