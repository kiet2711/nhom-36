package com.auction.network;

public enum CommandType {
    LOGIN, REGISTER,
    GET_AUCTIONS, GET_AUCTION_DETAIL,
    CREATE_AUCTION, PLACE_BID,
    CLOSE_AUCTION,
    UPDATE_PUSH,
    ERROR
}