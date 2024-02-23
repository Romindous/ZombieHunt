package ru.romindous.zh.Game;

public enum GameState {
	
	WAITING("LOBBY_WAIT"),
	LOBBY_START("LOBBY_START"),
	BEGINING("BEGINING"),
	RUNNING("RUNNING"),
	END("END");
	
	public final String state;
	
	GameState(String state) {
		this.state = state;
	}
}
