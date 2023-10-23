import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class Life{
    private final static int WIDTH = 800;
    private final static int HEIGHT = 800;
    private final static int BWIDTH = 20;
    private final static int BHEIGHT = 20;
    private final static int GRID_WIDTH = 1;
    private static float FRAME_TIME = 1;

    private static Color cellColor = Color.GREEN;
    private static Color gridColor = Color.WHITE;
    private static Color bgColor = Color.BLACK;

    public static Window window = new Window();
    public static GamePanel game = new GamePanel(WIDTH,HEIGHT,BWIDTH,BHEIGHT,GRID_WIDTH,FRAME_TIME,cellColor,gridColor,bgColor);

    public static void main(String[] args){
        window.add(game);
        window.pack();
        window.setResizable(false);
        window.setVisible(true);

        game.setFocusable(true);
        game.paintImmediately(0,0,WIDTH,HEIGHT);

        new Thread(game).start();
    }
}

class Window extends JFrame{

    Window(){
        super();
        setTitle("Conway's Life");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

}

class GamePanel extends JPanel implements Runnable,MouseListener{
    public int WIDTH,HEIGHT,BWIDTH,BHEIGHT,GRID_WIDTH;
    public float FRAME_TIME;

    public Color cellColor, gridColor;

    public int cellWidth;
    public Cell[] CELLS;
    public static boolean running = false;
    public PausePlayAction pausePlayAction = new PausePlayAction(this);

    GamePanel(int WIDTH, int HEIGHT, int BWIDTH, int BHEIGHT, int GRID_WIDTH, float FRAME_TIME, 
                Color cellColor, Color gridColor, Color bgColor){
        super();

        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        this.BWIDTH = BWIDTH;
        this.BHEIGHT = BHEIGHT;
        this.GRID_WIDTH = GRID_WIDTH;
        this.FRAME_TIME = FRAME_TIME;
        this.cellColor = cellColor;
        this.CELLS = new Cell[BWIDTH*BHEIGHT];

        this.cellWidth = Math.min(HEIGHT/BHEIGHT,WIDTH/BWIDTH);
        for (int i=0; i < BWIDTH; i++){
            for (int j=0; j < BHEIGHT; j++){
                CELLS[j*BWIDTH + i] = new Cell(i,j,cellWidth,GRID_WIDTH);
            }
        }

        setPreferredSize(new Dimension(WIDTH,HEIGHT));
        setBackground(bgColor);
        eventInit();
    }

    public int[][] getNeighbors(Cell cell){
        int X = cell.X;
        int Y = cell.Y;

        int[] xs,ys;

        if (X==0){
            xs = new int[]{0,1};
        } else if (X==BWIDTH-1){
            xs = new int[]{BWIDTH-2,BWIDTH-1};
        } else {
            xs = new int[]{X-1,X,X+1};
        }

        if (Y==0){
            ys = new int[]{0,1};
        } else if (Y==BHEIGHT-1){
            ys = new int[]{BHEIGHT-2,BHEIGHT-1};
        } else {
            ys = new int[]{Y-1,Y,Y+1};
        }

        int[][] result = {xs,ys};
        return result;
    }

    public void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D) g;
        super.paintComponent(g);
        g2.setColor(gridColor);
        g2.setStroke(new BasicStroke(2*GRID_WIDTH));
        for (int x=0;x<WIDTH;x+=cellWidth){
            g2.draw(new Line2D.Float(x,0,x,HEIGHT));
        }
        for (int y=0;y<HEIGHT;y+=cellWidth){
            g2.draw(new Line2D.Float(0,y,WIDTH,y));
        }
        g.setColor(cellColor);
        for (Cell cell : CELLS){
            cell.paint(g);
        }
    }

    public void update(){
        for (Cell cell : CELLS){
            int numNeighbors = 0;
            int[][] neighbors = getNeighbors(cell);
            int[] xs=neighbors[0];
            int[] ys=neighbors[1];

            for (int x : xs){
                for (int y : ys){
                    if (x==cell.X & y==cell.Y){
                        continue;
                    } else if (CELLS[y*BWIDTH+x].on){
                        numNeighbors += 1;
                    }
                }
            }

            if (cell.on){
                if (numNeighbors == 2 || numNeighbors == 3){
                    continue;
                } else {
                    cell.toggle();
                }
            } else if (numNeighbors == 3){
                cell.toggle();
            }
        }

        for (Cell cell : CELLS){
            cell.resolve();
        }
    }

    public void run(){
        double t = 0.0;
        double dt = 1_000_000_000;
        double currentTime = System.nanoTime();
        double accumulator = 0.0;
        
        while (running){
            double newTime=System.nanoTime();
            double frameTime = newTime-currentTime;
            currentTime = newTime;

            accumulator += frameTime;

            while ( accumulator >= dt ){
                update();
                t += dt;
                accumulator -= dt;
            }

            paintImmediately(0,0,WIDTH,HEIGHT);
        }
    }

    public void eventInit(){
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0),"bleh");
        actionMap.put("bleh", pausePlayAction);

        addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int gridX = x/cellWidth;
        int gridY = y/cellWidth;
        int index = BWIDTH*gridY + gridX;
        CELLS[index].toggle();
        CELLS[index].resolve();
        paintImmediately(0,0,WIDTH,HEIGHT);
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}

class Cell{
    public int X,Y,screenX,screenY,cellWidth;
    public boolean on = false;
    public boolean new_state = false;

    Cell(int X,int Y,int cellWidth,int offset){
        this.X = X;
        this.Y = Y;

        this.screenX = cellWidth*X+offset;
        this.screenY = cellWidth*Y+offset;

        this.cellWidth=cellWidth-2*offset;
    }

    public void toggle(){
        if (on){
            new_state = false;
        }else{
            new_state = true;
        }
    }

    public void resolve(){
        on = new_state;
    }

    public void paint(Graphics g){
        if (this.on){
            g.fillRect(screenX,screenY,cellWidth,cellWidth);
        }
    }
}

class PausePlayAction extends AbstractAction{
    public GamePanel game;

    public PausePlayAction(GamePanel game){
        super();
        this.game = game;
    }

    public void actionPerformed(ActionEvent e) {
        if (game.running){
            game.running = false;
        } else {
            game.running = true;
            new Thread(game).start();
        }
    }
}

