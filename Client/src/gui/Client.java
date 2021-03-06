package gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Optional;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Client {

    static Player player;
    String errorMessage;
    Socket clientSocket;
    BufferedReader dis;
    PrintStream ps;
    Boolean keepRunning;
    JSONObject sendJson;
    JSONObject recieveJson;
    String playerName;
    int r, c, score;
    private Vector<JSONObject> goldPlayers = new Vector<JSONObject>();
    private Vector<JSONObject> silverPlayers = new Vector<JSONObject>();
    private Vector<JSONObject> bronzePlayers = new Vector<JSONObject>();

    public Client() {
        player = new Player();
        sendJson = new JSONObject();
        try {
            keepRunning = true;
            clientSocket = new Socket("7.7.7.28", 5008);
            dis = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            ps = new PrintStream(clientSocket.getOutputStream());
            setErrorMessage("");
            Thread th = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (keepRunning) {

                        try {
                            System.out.println("client !");
                            String msg = dis.readLine();
                            System.out.println(msg);
                            recieveJson = new JSONObject(msg);
                            String type = (String) recieveJson.get("type");
                            System.out.println(type);

                            switch (type) {
                                case "login":
                                    handleLogin();
                                    break;
                                case "playerlist":
                                    fillPlayersVectors();
                                    break;
                                case "invite":
                                    getInviterInfo();
                                    break;
                                case "register":
                                    handleRegister();
                                    break;
                                case "responsetoinvite":
                                    getInviteResponse();
                                    break;
                                case "ingame":
                                    handleInGame();
                                    break;
                                case "win":
                                    handleLosing();
                                    break;
                                case "endofbattle":
                                    endOfBattleHandle();
                                    break;
                                case "score":
                                    setOtherScore();
                                    break;
                                case "draw":
                                    handleDraw();
                                    break;
                                case "stop":
                                	handleServerStop();
                            }
                        } catch (IOException ex) {
                            handleServerStop();

                        } catch (JSONException e) {
                        	
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }

            });
            th.start();
        } catch (IOException e) {
            // System.out.print("hi");
            // e.printStackTrace();
            keepRunning = false;

            stopConnection();
            Platform.runLater(new Runnable() {

                @Override

                public void run() {

                    //Update UI here    
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setHeaderText("Server is not running please restart");
                    alert.setContentText(null);
                    Optional<ButtonType> btnType = alert.showAndWait();
                    Main.stg.close();
                    if (btnType.get() == ButtonType.OK) {

                        System.exit(0);

                    } else {
                        System.exit(0);
                    }
                    Platform.exit();

                }

            });
            // System.exit(0);
        }

    }

    protected void getInviterInfo() {
        try {
            playerName = (String) recieveJson.get("username");
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        Platform.runLater(new Runnable() {

            @Override

            public void run() {
                //Update UI here    

                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setHeaderText(playerName + " wants to play with you, Do you want to accept the invitation?");
                alert.setContentText(null);
                Optional<ButtonType> btnType = alert.showAndWait();
                if (btnType.get() == ButtonType.OK) {
                    try {
                        //         Scene scene = new Scene(new Label("Hello"), 400, 500);
//                    Stage stage = (Stage) HomePageController.playersMenuUI.getScene().getWindow();
//                    stage.setScene(scene);
                        HomePageController.getGameControl().setoCount(player.getScore());
                        respondeToInvite(player.getName(), playerName, true);
                    } catch (JSONException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    try {
                        respondeToInvite(player.getName(), playerName, false);
                    } catch (JSONException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    public Vector<JSONObject> getGoldPlayers() {
        return goldPlayers;
    }

    public Vector<JSONObject> getSilverPlayers() {
        return silverPlayers;
    }

    public Vector<JSONObject> getBronzePlayers() {
        return bronzePlayers;
    }

    public void fillPlayersVectors() {
        try {
            JSONArray goldList = recieveJson.getJSONArray("Gold");
            JSONArray silverList = recieveJson.getJSONArray("Silver");
            JSONArray bronzeList = recieveJson.getJSONArray("bronze");

            Vector<JSONObject> gold = new Vector<JSONObject>();
            Vector<JSONObject> silver = new Vector<JSONObject>();
            Vector<JSONObject> bronze = new Vector<JSONObject>();

            for (int i = 0; i < goldList.length(); i++) {
                gold.add(goldList.getJSONObject(i));
            }
            for (int i = 0; i < silverList.length(); i++) {
                silver.add(silverList.getJSONObject(i));
            }
            for (int i = 0; i < bronzeList.length(); i++) {
                bronze.add(bronzeList.getJSONObject(i));
            }

            goldPlayers = gold;
            silverPlayers = silver;
            bronzePlayers = bronze;

            System.out.println("values " + bronzePlayers.size());

            System.out.println(HomePageController.playersMenuControl == null);

            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    if (HomePageController.playersMenuControl != null
                            && HomePageController.playersMenuControl.getTable() != null) {
                        System.out.println("tableeeeeeeee");
                        HomePageController.playersMenuControl.getTable().refresh();
                        //	PlayersMenuController.table.refresh();
                    }
                }

            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void handleLogin() {
        String result = null;
        try {
            result = (String) recieveJson.get("res");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (result.equals("Successfully")) {
            setPlayer();
        } else {
            player.setId(-1);
        }
    }

    public void handleRegister() {
        String result = null;
        setErrorMessage("");
        try {
            result = (String) recieveJson.get("res");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (result.equals("Successfully")) {
            setErrorMessage("");
        } else if (result.equals("failed")) {
            System.out.println(result);
            setErrorMessage("Either username or email is duplicate");
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer() {
        try {
            player.setName((String) recieveJson.get("name"));
            player.setScore((int) recieveJson.get("score"));
            player.setId(recieveJson.getInt("id"));
        } catch (JSONException e) {
            e.printStackTrace();
            // System.out.println("");
        }

    }

    public void stopConnection() {
        try {
            clientSocket.close();
        } catch (IOException ex) {
            System.out.print("hi"); //
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException e) {
            System.out.print("closed");
        }
    }

    public void setPlayerToZero() {
        player.setName("");
        player.setScore(0);
        player.setId(0);
        player.setPlayingWith("");
    }

    public void login(String userName, String password) throws JSONException {
        sendJson.put("type", "login");
        sendJson.put("username", userName);
        sendJson.put("password", password);
        sendToServer();

    }

    public void invite(String userName1, String userName2) throws JSONException {
        System.out.println("================inviteeeeee");
        JSONObject send = new JSONObject();
        send.put("type", "invite");
        send.put("askingplayername", userName1);
        send.put("toPlayWith", userName2);
        sendJson = send;
        sendToServer();
    }

    public void respondeToInvite(String userName1, String userName2, boolean inviteStatus) throws JSONException {
        sendJson.put("type", "responsetoinvite");
        sendJson.put("toPlayWith", userName2);
        sendJson.put("username", userName1);
        if (inviteStatus) {
            sendJson.put("response", "accept");
            GameController.setTurn(false);
            HomePageController.vsComputer=false;
            player.setPlayingWith(userName1);
            GameController.setThePlayer("O");
            sendToServer();
            HomePageController.getGameControl().setUsernameOne(userName2);
            HomePageController.getGameControl().setUsernameTwo(userName1);
            HomePageController.getGameControl().setScoreplayerO(Integer.toString(player.getScore()));

            HomePageController.loadGame();
        } else {
            sendJson.put("response", "rejected");
             sendToServer();
        }
       
    }

    public void register(String userName, String password, String email) throws JSONException {
        sendJson.put("type", "register");
        sendJson.put("username", userName);
        sendJson.put("password", password);
        sendJson.put("email", email);
        sendToServer();

    }

    public void stop() throws JSONException {
        sendJson.put("type", "stop");
        sendToServer();
    }

    public void logout() throws JSONException {
        System.out.println("logout");
        sendJson.put("type", "logout");
        sendToServer();

    }

    public void endOfGame() throws JSONException {
        sendJson.put("type", "endofgame");
        sendJson.put("score", player.getScore());
        System.out.println("==============" + player.getScore() + "=====================");
        System.out.print(sendJson);
        sendToServer();
    }

    public void sendToServer() {
        System.out.println(sendJson);
        ps.println(sendJson);
    }

    public void setErrorMessage(String error) {
        errorMessage = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void getInviteResponse() {
        System.out.println("=========response to invite========");
        try {

            if (recieveJson.get("response").toString().equals("accept")) {
                player.setScoreOfOpponent((int) recieveJson.get("score"));
                Platform.runLater(new Runnable() {

                    @Override

                    public void run() {
                        HomePageController.loadGame();
                        GameController.setTurn(true);
                        HomePageController.getGameControl().setUsernameOne(player.getName());
                        try {
                            HomePageController.getGameControl().setUsernameTwo((String) recieveJson.get("username"));
                            HomePageController.getGameControl().setScoreplayerO(Integer.toString((int) recieveJson.get("score")));
                            HomePageController.getGameControl().setScoreplayerX(Integer.toString(player.getScore()));
                            HomePageController.getGameControl().setoCount((int) recieveJson.get("score"));
                            HomePageController.getGameControl().setxCount(player.getScore());

                        } catch (JSONException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        GameController.setThePlayer("X");
//                    /  GameController.setUsernameOne(player.getName());
                        System.out.println("------------------getInvite");
                    }

                });
            }
        } catch (JSONException ex) {
            System.out.println("errrrrrorr");
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    public void inGame(int row, int col) {
        JSONObject send = new JSONObject();
        try {
            send.put("type", "ingame");
            send.put("row", row);
            send.put("column", col);
            sendJson = send;
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendToServer();
    }

    public void handleInGame() {
        GameController.setTurn(true);

        try {
            r = (Integer) recieveJson.get("row");
            c = (Integer) recieveJson.get("column");

        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        Platform.runLater(new Runnable() {

            @Override

            public void run() {
                //Update UI here    
                HomePageController.getGameControl().placeMoveOnGrid(r, c);
            }
        });

    }

    public void win() {
        try {
            JSONObject win = new JSONObject();
            try {
                win.put("type", "win");
            } catch (JSONException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            if ("X".equals(GameController.thePlayer)) {
                win.put("score", HomePageController.getGameControl().getxCount());
            } else {
                win.put("score", HomePageController.getGameControl().getoCount());
            }

            sendJson = win;
            sendToServer();
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void handleLosing() {
        System.out.println("------lost--------");
        System.out.println(recieveJson);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //Update UI here    
                System.out.println("------lost--------");
                HomePageController.getGameControl().reset();
                System.out.println(recieveJson);
                if (GameController.thePlayer.equals("X")) {
                    System.out.println("i am heeeeeeeeeeeeeeeeeeeeeeeerrrrrrrrrrrrrreeeeeeeeeeeee");
                    try {
                        HomePageController.getGameControl().setScoreplayerO(Integer.toString(recieveJson.getInt("score") + 20));
                        HomePageController.getGameControl().setoCount(recieveJson.getInt("score") + 20);
                    } catch (JSONException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else {

                    System.out.println("no iaaaam hereeeeeeeeeeeeee");
                    try {
                        HomePageController.getGameControl().setScoreplayerX(Integer.toString(recieveJson.getInt("score") + 20));
                        HomePageController.getGameControl().setxCount((int) recieveJson.get("score") + 20);
                    } catch (JSONException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setHeaderText("You Lost");

                alert.setContentText(null);
                Optional<ButtonType> btnType = alert.showAndWait();
                if (btnType.get() == ButtonType.OK) {

                }
            }
        });
    }

    public void endOfBattle() {
        try {
            endOfGame();
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        JSONObject end = new JSONObject();
        try {
            end.put("type", "endofbattle");
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendJson = end;
        sendToServer();
    }

    public void endOfBattleHandle() {
        Platform.runLater(new Runnable() {

            @Override

            public void run() {

                //Update UI here    
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("Other player exits the game");
                alert.setContentText(null);
                Optional<ButtonType> btnType = alert.showAndWait();
                // Main.stg.close();
                if (btnType.get() == ButtonType.OK) {
                    FXMLLoader homePageLoader = new FXMLLoader(getClass().getResource("HomePage.fxml"));
                    Parent homePageUI = null;
                    HomePageController homePageControl = null;
                    Main.client.endOfBattle();
                    try {
                        homePageUI = homePageLoader.load();
                        homePageControl = (HomePageController) homePageLoader.getController();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    HomePageController.stg.setScene(new Scene(homePageUI));
                    homePageControl.setActionHandler(HomePageController.stg);

                } else {

                }

            }

        });
    }

    public void setOtherScore() {
        System.out.println("set other scoreee");
        System.out.println(recieveJson);

        try {
            score = (int) recieveJson.get("score");
            System.out.println(score);
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //Update UI here    

                HomePageController.getGameControl().setxCount(score);
                HomePageController.getGameControl().setScoreplayerX(Integer.toString(score));
                Main.client.getPlayer().setScoreOfOpponent(score);

            }
        });

    }

    public void draw() {
        JSONObject draw = new JSONObject();
        try {
            draw.put("type", "draw");
        } catch (JSONException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        sendJson = draw;
        sendToServer();

    }

    public void handleDraw() {
        Platform.runLater(new Runnable() {

            @Override

            public void run() {
                //Update UI here    
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setHeaderText("No One Wins");
                alert.setContentText(null);
                Optional<ButtonType> btnType = alert.showAndWait();

                if (btnType.get() == ButtonType.OK) {
                    HomePageController.getGameControl().reset();
                } else {
                    HomePageController.getGameControl().reset();
                }
            }
        });
    }

    public void handleServerStop() {
        keepRunning = false;
        System.out.print("server has closed");
        Platform.runLater(new Runnable() {

            @Override

            public void run() {
                //Update UI here    
                Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("Server has stopped, please restart");
                alert.setContentText(null);
                Optional<ButtonType> btnType = alert.showAndWait();
                Main.stg.close();

                if (btnType.get() == ButtonType.OK) {
                    System.exit(0);
                } else {
                    System.exit(0);
                }
            }
        });
    }
}
