create schema kraken;

create type kraken.action as enum (
  'buy',
  'sell'
);

create type kraken.order_type as enum (
  'limit',
  'market'
);

create table kraken.trade (
  transaction_id text not null,
  order_transaction_id text not null,
  pair text not null,
  timestamp timestamptz not null,
  action kraken.action not null,
  order_type kraken.order_type not null,
  price numeric not null,
  cost numeric not null,
  fee numeric not null,
  volume numeric not null,
  constraint trade_pkey primary key (transaction_id)
);

create index on kraken.trade (timestamp, pair);

