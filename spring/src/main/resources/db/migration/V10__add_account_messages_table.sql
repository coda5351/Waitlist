-- add table to store account-specific message templates
CREATE TABLE account_messages (
    account_id bigint not null,
    message_key varchar(100) not null,
    template text,
    primary key (account_id, message_key),
    foreign key (account_id) references accounts(id)
);