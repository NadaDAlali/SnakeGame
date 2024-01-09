package com.game.snakegame;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

public class SnakeGame extends JPanel {
    // Variables
    static int speed = 6;
    static int width = 20;
    static int height = 20;
    static int cornersize = 25;
    static Stack<Corner> snake = new Stack<>();
    static Deque<Food> foods = new ArrayDeque<>();
    static Dir direction = Dir.right;
    static boolean gameOver = false;
    static Random rand = new Random();
    static int highScore = 0;
    static final String DIR = System.getProperty("user.dir");
    public JPanel scorePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private JLabel scoreLabel = new JLabel("Score: ");
    private JLabel highScoreLabel = new JLabel();
    private Timer timer;
    private boolean paused = false;
    private Image splashScreenImage;

    public enum Dir {
        left, right, up, down
    }

    public class Corner {
        int x;
        int y;

        public Corner(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public class Food extends Corner {
        private final FoodType type;

        public Food(int x, int y, FoodType type) {
            super(x, y);
            this.type = type;
        }

        public FoodType getType() {
            return type;
        }
    }

    public enum FoodType {
        GREEN,
        RED
    }

    public SnakeGame() {
        setPreferredSize(new Dimension(width * cornersize, height * cornersize));
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        scoreLabel.setFont(new Font("", Font.BOLD, 18));
        scoreLabel.setForeground(Color.BLACK);
        highScoreLabel.setFont(new Font("", Font.BOLD, 18));
        highScoreLabel.setForeground(Color.BLACK);

        scorePanel.setBackground(Color.white);
        final int padding = 10;
        scorePanel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        scorePanel.add(scoreLabel);
        scorePanel.add(highScoreLabel);

        add(scorePanel, BorderLayout.NORTH);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (splashScreenImage != null) {
                    startGameFromSplashScreen();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP && direction != Dir.down) {
                    direction = Dir.up;
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT && direction != Dir.right) {
                    direction = Dir.left;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN && direction != Dir.up) {
                    direction = Dir.down;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT && direction != Dir.left) {
                    direction = Dir.right;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    togglePause();
                }
            }
        });

        setFocusable(true);
    }

    public void startGameFromSplashScreen() {
        splashScreenImage = null; // Hide splash screen image
        startGame(); // Start the game
    }

    public void startGame() {

        if (timer != null && timer.isRunning())
            return;

        loadHighScore();
        newFood();
        newFood();

        // Add starting snake parts
        snake.push(new Corner(width / 2, height / 2));
        snake.push(new Corner(width / 2, height / 2));
        snake.push(new Corner(width / 2, height / 2));

        timer = new Timer(1000 / speed, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver) {
                    ((Timer) e.getSource()).stop();
                    stopGame();
                    repaint();
                    return;
                }
                tick();
                repaint();
            }
        });

        timer.start();
    }

    public void start() {
        timer = new Timer(1000 / speed, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver) {
                    ((Timer) e.getSource()).stop();
                    return;
                }
                tick();
                repaint();
            }
        });

        timer.start();
    }

    public void togglePause() {
        if (gameOver) {
            return; // If the game is already over, do nothing
        }

        paused = !paused;

        if (!paused) {
            start();
        } else {
            stopGame();
        }
    }

    public void stopGame() {
        if (timer != null)
            timer.stop();
        saveHighScore();
    }

    public void tick() {
        if (gameOver) {
            return;
        }

        for (int i = snake.size() - 1; i >= 1; i--) {
            snake.get(i).x = snake.get(i - 1).x;
            snake.get(i).y = snake.get(i - 1).y;
        }

        switch (direction) {
            case up:
                snake.get(0).y--;
                if (snake.get(0).y <= 0) {
                    gameOver = true;
                }
                break;
            case down:
                snake.get(0).y++;
                if (snake.get(0).y >= height) {
                    gameOver = true;
                }
                break;
            case left:
                snake.get(0).x--;
                if (snake.get(0).x <= 0) {
                    gameOver = true;
                }
                break;
            case right:
                snake.get(0).x++;
                if (snake.get(0).x >= width) {
                    gameOver = true;
                }
                break;
        }

        // Eat food
        Iterator<Food> foodIterator = foods.iterator();
        while (foodIterator.hasNext()) {
            Food food = foodIterator.next();
            if (snake.get(0).x == food.x && snake.get(0).y == food.y) {
                if (food.getType() == FoodType.GREEN) {
                    snake.push(new Corner(-1, -1));
                    speed++;
                } else if (food.getType() == FoodType.RED) {
                    if (snake.size() > 3) {
                        snake.pop();
                        speed--;
                    }
                }
                foodIterator.remove();
                newFood();
                break;
            }
        }

        // Self-destroy
        for (int i = 1; i < snake.size(); i++) {
            if (snake.get(0).x == snake.get(i).x && snake.get(0).y == snake.get(i).y) {
                gameOver = true;
                break;
            }
        }

        // Update high score
        int currentScore = speed - 6;
        if (currentScore > highScore) {
            highScore = currentScore;
        }

        // Display score
        scoreLabel.setText("Score: " + currentScore);
        highScoreLabel.setText("High Score: " + highScore);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw splash screen image
        if (splashScreenImage != null) {
            g2d.drawImage(splashScreenImage, 0, 0, getWidth(), getHeight(), this);
            return;
        }

        // Draw foods
        for (Food food : foods) {
            if (food.getType() == FoodType.GREEN) {
                g2d.setColor(Color.GREEN);
            } else if (food.getType() == FoodType.RED) {
                g2d.setColor(Color.RED);
            }
            g2d.fillOval(food.x * cornersize, food.y * cornersize, cornersize, cornersize);
        }

        // Draw snake
        for (Corner c : snake) {
            g2d.setColor(Color.GREEN);
            g2d.fillRect(c.x * cornersize, c.y * cornersize, cornersize - 1, cornersize - 1);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(c.x * cornersize, c.y * cornersize, cornersize - 2, cornersize - 2);
        }

        // Draw game over message
        if (gameOver) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("", Font.BOLD, 50));
            g2d.drawString("GAME OVER", 100, 250);
            saveHighScore();
        }

        if (paused) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("", Font.BOLD, 50));
            g2d.drawString("Paused", 100, 250);
        }

        // Draw red border
        g2d.setColor(Color.RED);
        int borderWidth = 10;
        int borderArcSize = 10;
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, borderArcSize, borderArcSize);

    }

    // Generate new food
    public void newFood() {
        int foodX;
        int foodY;

        while (true) {
            foodX = rand.nextInt(width);
            foodY = rand.nextInt(height);
            boolean foodExists = false;
            for (Corner c : snake) {
                if (c.x == foodX && c.y == foodY) {
                    foodExists = true;
                    break;
                }
            }

            if (!foodExists) {
                // Check if there is already a food of the same color
                boolean isGreenFoodExists = foods.stream().anyMatch(food -> food.getType() == FoodType.GREEN);
                boolean isRedFoodExists = foods.stream().anyMatch(food -> food.getType() == FoodType.RED);

                FoodType type;
                if (isGreenFoodExists && isRedFoodExists)
                    type = rand.nextBoolean() ? FoodType.GREEN : FoodType.RED;
                else if (isGreenFoodExists)
                    type = FoodType.RED;
                else
                    type = FoodType.GREEN;
                foods.add(new Food(foodX, foodY, type));
                break;
            }
        }
    }

    // Save high score to a file
    public void saveHighScore() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DIR + "\\src\\highscore.txt"));
            writer.write(String.valueOf(highScore));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load high score from a file
    public void loadHighScore() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DIR + "\\src\\highscore.txt"));
            String line = reader.readLine();
            if (line != null && !line.isEmpty()) {
                highScore = Integer.parseInt(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Snake Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        SnakeGame snakeGame = new SnakeGame();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(snakeGame, BorderLayout.CENTER);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(snakeGame.scorePanel, BorderLayout.NORTH);
        contentPane.add(snakeGame, BorderLayout.CENTER);
        frame.setContentPane(contentPane);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Load splash screen image
        try {
            final String path = DIR + "\\src\\SNEE.jpeg";
            snakeGame.splashScreenImage = ImageIO.read(new File(path)); // Replace with your splash screen image file name
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

    }
}
