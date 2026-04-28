CREATE TABLE IF NOT EXISTS users (
                                     id       TEXT PRIMARY KEY,
                                     username TEXT UNIQUE NOT NULL,
                                     password TEXT NOT NULL,
                                     role     TEXT NOT NULL  -- BIDDER | SELLER | ADMIN
);

CREATE TABLE IF NOT EXISTS items (
                                     id            TEXT PRIMARY KEY,
                                     name          TEXT NOT NULL,
                                     description   TEXT,
                                     type          TEXT NOT NULL,  -- ELECTRONICS | ART | VEHICLE
                                     starting_price REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS auctions (
                                        id             TEXT PRIMARY KEY,
                                        item_id        TEXT NOT NULL,
                                        seller_id      TEXT NOT NULL,
                                        current_price  REAL NOT NULL,
                                        leading_bidder TEXT,
                                        status         TEXT NOT NULL,  -- OPEN | RUNNING | FINISHED | CANCELED
                                        start_time     TEXT NOT NULL,
                                        end_time       TEXT NOT NULL,
                                        FOREIGN KEY (item_id)    REFERENCES items(id),
    FOREIGN KEY (seller_id)  REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS bid_transactions (
                                                id         TEXT PRIMARY KEY,
                                                auction_id TEXT NOT NULL,
                                                bidder_id  TEXT NOT NULL,
                                                amount     REAL NOT NULL,
                                                bid_time   TEXT NOT NULL,
                                                FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id)  REFERENCES users(id)
    );