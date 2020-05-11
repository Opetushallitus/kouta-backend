alter table roles rename to authorities;
alter table authorities rename role to authority;
alter table authorities rename constraint "roles_session_fkey" to "authorities_session_fkey";
