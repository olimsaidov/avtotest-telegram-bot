create table answer
(
  message_id int,
  user_id integer references users,
  section int,
  sub_section int,
  question int,
  created_at timestamp with time zone,
  guessed_at timestamp with time zone,
  tries jsonb,
  primary key(message_id, user_id)
);



