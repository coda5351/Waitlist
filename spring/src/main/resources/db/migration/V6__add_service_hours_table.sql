-- add table to store service hours per account
CREATE TABLE account_service_hours (
    account_id bigint not null,
    day_of_week varchar(20) not null,
    open_time time,
    close_time time,
    primary key (account_id, day_of_week),
    foreign key (account_id) references accounts(id)
);
