package com.github.zuihou.auth.utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;

import com.github.zuihou.common.excode.ExceptionCode;
import com.github.zuihou.context.BaseContextConstants;
import com.github.zuihou.exception.BizException;
import com.github.zuihou.utils.DateUtils;
import com.github.zuihou.utils.StringHelper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by ace on 2017/9/10.
 *
 * @author zuihou
 * @date 2019-06-20 22:20
 */
@Slf4j
public class JwtHelper {

    private static final RsaKeyHelper rsaKeyHelper = new RsaKeyHelper();


    /**
     * 生成用户token
     *
     * @param jwtInfo
     * @param priKeyPath
     * @param expire
     * @return
     * @throws BizException
     */
    public static Token generateUserToken(JwtUserInfo jwtInfo, String priKeyPath, int expire) throws BizException {
        JwtBuilder jwtBuilder = Jwts.builder()
                //设置主题
                .setSubject(String.valueOf(jwtInfo.getUserId()))
                .claim(BaseContextConstants.JWT_KEY_ACCOUNT, jwtInfo.getAccount())
                .claim(BaseContextConstants.JWT_KEY_NICK_NAME, jwtInfo.getNickName())
                .claim(BaseContextConstants.JWT_KEY_ORG_ID, jwtInfo.getOrgId())
                .claim(BaseContextConstants.JWT_KEY_DEPARTMENT_ID, jwtInfo.getDepartmentId())
                .claim(BaseContextConstants.JWT_KEY_ACCOUNT_TYPE, jwtInfo.getAccountType());
        return generateToken(jwtBuilder, priKeyPath, expire);
    }

    /**
     * 获取token中的用户信息
     *
     * @param token      token
     * @param pubKeyPath 公钥路径
     * @return
     * @throws Exception
     */
    public static JwtUserInfo getJwtFromToken(String token, String pubKeyPath) throws BizException {
        Jws<Claims> claimsJws = parserToken(token, pubKeyPath);
        Claims body = claimsJws.getBody();
        String strUserId = body.getSubject();
        String account = StringHelper.getObjectValue(body.get(BaseContextConstants.JWT_KEY_ACCOUNT));
        String nickName = StringHelper.getObjectValue(body.get(BaseContextConstants.JWT_KEY_NICK_NAME));

        String orgId = StringHelper.getObjectValue(body.get(BaseContextConstants.JWT_KEY_ORG_ID));
        String departmentId = StringHelper.getObjectValue(body.get(BaseContextConstants.JWT_KEY_DEPARTMENT_ID));
        String accountType = StringHelper.getObjectValue(body.get(BaseContextConstants.JWT_KEY_ACCOUNT_TYPE));
        Long userId = null;
        try {
            userId = Long.parseLong(strUserId);
        } catch (Exception e) {
            log.info("解析jwt中用户id出错", e);
        }
        return new JwtUserInfo(userId, account, nickName, orgId, departmentId, accountType);
    }


    /**
     * 生成token
     *
     * @param builder
     * @param priKeyPath
     * @param expire
     * @return
     * @throws BizException
     */
    protected static Token generateToken(JwtBuilder builder, String priKeyPath, int expire) throws BizException {
        try {
            //返回的字符串便是我们的jwt串了
            String compactJws = builder.setExpiration(DateUtils.localDateTime2Date(LocalDateTime.now().plusSeconds(expire)))
                    //设置算法（必须）
                    .signWith(SignatureAlgorithm.RS256, rsaKeyHelper.getPrivateKey(priKeyPath))
                    //这个是全部设置完成后拼成jwt串的方法
                    .compact();
            return new Token(compactJws, expire);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("errcode:{}, message:{}", ExceptionCode.JWT_GEN_TOKEN_FAIL.getCode(), e.getMessage());
            throw new BizException(ExceptionCode.JWT_GEN_TOKEN_FAIL.getCode(), ExceptionCode.JWT_GEN_TOKEN_FAIL.getMsg());
        }
    }


    /**
     * 公钥解析token
     *
     * @param token
     * @param pubKeyPath 公钥路径
     * @return
     * @throws Exception
     */
    private static Jws<Claims> parserToken(String token, String pubKeyPath) throws BizException {
        try {
            return Jwts.parser().setSigningKey(rsaKeyHelper.getPublicKey(pubKeyPath)).parseClaimsJws(token);
        } catch (ExpiredJwtException ex) {
            //过期
            throw new BizException(ExceptionCode.JWT_TOKEN_EXPIRED.getCode(), ExceptionCode.JWT_TOKEN_EXPIRED.getMsg());
        } catch (SignatureException ex) {
            //签名错误
            throw new BizException(ExceptionCode.JWT_SIGNATURE.getCode(), ExceptionCode.JWT_SIGNATURE.getMsg());
        } catch (IllegalArgumentException ex) {
            //token 为空
            throw new BizException(ExceptionCode.JWT_ILLEGAL_ARGUMENT.getCode(), ExceptionCode.JWT_ILLEGAL_ARGUMENT.getMsg());
        } catch (Exception e) {
            log.error("errcode:{}, message:{}", ExceptionCode.JWT_PARSER_TOKEN_FAIL.getCode(), e.getMessage());
            throw new BizException(ExceptionCode.JWT_PARSER_TOKEN_FAIL.getCode(), ExceptionCode.JWT_PARSER_TOKEN_FAIL.getMsg());
        }
    }
}
