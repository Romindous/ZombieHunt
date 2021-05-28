package me.Romindous.ZombieHunt.Game;

public enum GameState {
	
	LOBBY_WAIT("LOBBY_WAIT"),
	LOBBY_START("LOBBY_START"),
	BEGINING("BEGINING"),
	RUNNING("RUNNING"),
	END("END");
	
	public String state;
	
	private GameState(String state) {
		this.state = state;
	}
}
