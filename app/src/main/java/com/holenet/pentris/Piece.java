package com.holenet.pentris;

import java.util.Objects;

public class Piece{
    static int initVert[][][] = {
            {{0,0}, {0,0}, {0,0}, {0,0}, {0,0}},
            {{3,1}, {2,0}, {3,0}, {2,-1}, {3,-1}},      // I
            {{2,0}, {1,0}, {0,0}, {1,-1}, {2,-1}},      // C
            {{0,1}, {1,0}, {0,0}, {1,-1}, {2,-1}},      // L
            {{3,1}, {2,0}, {3,0}, {2,-1}, {1,-1}},      // L'
            {{0,1}, {2,0}, {1,0}, {0,0}, {1,-1}},       // P
            {{3,1}, {1,0}, {2,0}, {3,0}, {2,-1}},       // P'
    };
    static int rot[][] = {{0,0}, {-1,0}, {1,0}, {0,-1}, {2,0}, {3,-1}, {3,0}, {4,0}, {2,1}, {3,1}, {1,1}, {0,1}};

    int tri[][];
    int x,y;
    int color;
    int theta;
    int center;

    public Piece() {
        tri = new int[5][2];
    }

    public Piece(int x, int y, int color, int r) {
        tri = new int[5][2];
        for(int i=0; i<5; i++) {
            tri[i][0] = initVert[color][i][0];
            tri[i][1] = initVert[color][i][1];
        }
        this.x = x;
        this.y = y;
        this.color = color;
        theta = 0;

        for(int i=0; i<r; i++)
            rotate(1);
        center = 0;
        for(int i=0; i<5; i++) {
            if(tri[i][0]<=-1)
                center = -1;
            else if(tri[i][0]>=4)
                center = 1;
        }
    }

    public void copy(Piece a) {
        for(int i=0; i<5; i++)
            System.arraycopy(a.tri[i], 0, tri[i], 0, 2);
        x = a.x;
        y = a.y;
        color = a.color;
        center = a.center;
        theta = a.theta;
    }

    public void trans(int d) {
        if(d%2==0) {
            y += d-1;
        } else {
            y += x%4-1;
            x += d*2;
            for(int i=0; i<5; i++) {
                if((tri[i][0]+3)%4<2)
                    tri[i][1] += x%4-1;
            }
        }
    }

    public void rotate(int h) {
        int sgn = x%4-1;
        for(int i=0; i<5; i++) {
            int j;
            for(j=0; j<12; j++) {
                if(tri[i][0]==rot[j][0] && sgn*tri[i][1]==rot[j][1]) {
                    break;
                }
            }
            if(j<12) {
                tri[i][0] = rot[(j+12+h*sgn*2)%12][0];
                tri[i][1] = sgn*rot[(j+12+h*sgn*2)%12][1];
            }
        }
        theta += h;
    }
}