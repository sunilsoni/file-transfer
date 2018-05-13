package com.ssh.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.JSch;
import org.vngx.jsch.Session;
import org.vngx.jsch.config.SessionConfig;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.userauth.UserAuth;

import java.nio.charset.StandardCharsets;

public interface SshSessionFactory {

    Session createSession();

    ChannelSftp getSftpChannel() throws JSchException;

    default void closeSession(Session session) {
        if (session != null) {
            session.disconnect();
        }
    }

    @Slf4j
    @Service("sessionFactory")
    class JschSessionFactory implements SshSessionFactory {

        @Override
        public Session createSession() {
            try {
                String username = System.getenv("ssh.user.name");
                String host = System.getenv("ssh.host");
                int port = Integer.parseInt(System.getenv("ssh.port"));
                String password = System.getenv("ssh.user.password");
                log.info("Creating session factory with username: {}, host: {} , and port: {}", username, host, port);

                JSch jsch = JSch.getInstance();
                Session session = jsch.createSession(username, host, port);
                session.getConfig().setProperty(SessionConfig.STRICT_HOST_KEY_CHECKING, "no");
                session.getConfig().setProperty(SessionConfig.PREFFERED_AUTHS, UserAuth.PASSWORD);

                session.connect(password.getBytes(StandardCharsets.UTF_8));
                return session;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public ChannelSftp getSftpChannel() throws JSchException {
            Session session = createSession();

            try {
                return (ChannelSftp) session.openChannel("sftp");
            } catch (JSchException e) {
                log.error("Error in creating  Channel", e);
                throw new JSchException();
            }
        }
    }
}
