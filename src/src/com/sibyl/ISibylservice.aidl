package com.sibyl;

interface ISibylservice {

    void start();
    /* start playing the current item in the selected playlist */
    
    void stop();
    /* stop playing */
    
    void pause();
    /* pause the song. To start playing the paused song at its current position
        use start */
    
    int getCurrentPosition();
    /* returns the current position in the played song in milliseconds */
    
    void setCurrentPosition(in int msec);
    /* sets the current position in the played song in milliseconds */
    
    int getDuration();
    /* returns the duration of the played song in milliseconds */
    
    void setLooping(in int looping);
    /* activate or deactivate looping / repetition of the current song 
        If looping is set to 0: the current song is played once 
        If looping is set to 1: the current song is repeated while looping is not 0 */
}