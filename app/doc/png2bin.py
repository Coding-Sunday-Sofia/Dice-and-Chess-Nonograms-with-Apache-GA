from PIL import Image
import sys

def png_to_binary(png_path, threshold=128):
    # Open PNG image
    img = Image.open(png_path)
    
    # Convert to grayscale
    img_gray = img.convert('L')
    
    # Thresholding
    img_binary = img_gray.point(lambda p: p > threshold and 255)
    
    # Convert to 0s and 1s
    img_binary = img_binary.point(lambda p: 0 if p // 255 else 1)
    
    # Convert image to list of lists (2D array)
    binary_matrix = list(img_binary.getdata())
    width, height = img_binary.size
    binary_matrix = [binary_matrix[i:i+width] for i in range(0, width*height, width)]
    
    return binary_matrix

def save_binary_matrix(binary_matrix, output_file):
    with open(output_file, 'w') as f:
        for row in binary_matrix:
            for pixel in row:
                f.write(str(pixel))
            f.write('\n')

if __name__ == "__main__":
    # Check if the correct number of arguments is provided
    if len(sys.argv) < 3:
        print("Usage: python script_name.py png_file output_file [threshold]")
        sys.exit(1)

    # Get the file names and threshold from command line arguments
    png_path = sys.argv[1]
    output_file = sys.argv[2]
    
    # Set threshold to default or use provided value
    threshold = 128
    if len(sys.argv) >= 4:
        threshold = int(sys.argv[3])

    # Convert PNG to binary
    binary_matrix = png_to_binary(png_path, threshold)
    
    # Save the binary matrix to a file
    save_binary_matrix(binary_matrix, output_file)
