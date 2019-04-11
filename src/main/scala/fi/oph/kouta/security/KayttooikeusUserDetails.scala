package fi.oph.kouta.security

case class KayttooikeusUserDetails(roles : Set[Role], oid: String)

case class KayttooikeusUserResp(authorities : List[GrantedAuthority], username: String)
case class GrantedAuthority(authority : String)
