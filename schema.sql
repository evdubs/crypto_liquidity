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

create table kraken.usd_reference_price (
  transaction_id text not null,
  base_currency_pair text null,
  base_currency_price numeric null,
  base_currency_volume numeric null,
  base_currency_timestamp timestamptz null,
  counter_currency_pair text null,
  counter_currency_price numeric null,
  counter_currency_volume numeric null,
  counter_currency_timestamp timestamptz null,
  constraint usd_reference_price_pkey primary key (transaction_id),
  constraint usd_reference_price_transaction_id_fkey foreign key (transaction_id) references kraken.trade(transaction_id)
);

create table kraken.capital_gain_loss (
	symbol text null,
	acquire_timestamp timestamptz null,
	acquire_transaction_id text not null,
	cost_basis numeric null,
	dispose_timestamp timestamptz null,
	dispose_transaction_id text not null,
	proceeds numeric null,
	gain_loss numeric null,
	constraint capital_gain_loss_pkey primary key (acquire_transaction_id, dispose_transaction_id),
	constraint capital_gain_loss_acquire_transaction_id_fkey foreign key (acquire_transaction_id) references kraken.trade(transaction_id),
	constraint capital_gain_loss_dispose_transaction_id_fkey foreign key (dispose_transaction_id) references kraken.trade(transaction_id)
);
