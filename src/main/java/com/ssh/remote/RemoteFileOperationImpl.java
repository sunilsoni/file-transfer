package com.ssh.remote;


import com.ssh.config.SshSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vngx.jsch.ChannelSftp;
import org.vngx.jsch.SftpATTRS;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

@Slf4j
@Service("remoteFileOperation")
public class RemoteFileOperationImpl implements RemoteFileOperation {

    @Autowired
    SshSessionFactory sshSessionFactory;

    @Override
    public void mkdir(String path) throws SftpException, JSchException {

        ChannelSftp channelSftp = sshSessionFactory.getSftpChannel();
        log.info("channelSftp: {}", channelSftp);

        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("path cannot be blank");
        }


        String pwd = channelSftp.pwd();
        log.info("pwd: {}", pwd);

        String[] parts = path.split("/");
        if (StringUtils.isBlank(parts[0])) {
            parts[0] = "/";
        }
        log.info("parts: {}", parts[0]);


        for (String part : parts) {
            if (notExists(part)) {
                mkdir(part);
            }
            cd(part);
        }
        // return to original path before recursively create the new path
        cd(pwd);
    }

    @Override
    public void cd(String part) throws SftpException {
        if (StringUtils.isBlank(part)) {
            throw new IllegalArgumentException("part cannot be blank");
        }

        try {
            sshSessionFactory.getSftpChannel().cd(part);
        } catch (JSchException ex) {
            error("cd", part);
        } catch (SftpException e) {
            error("mkdir", part);
            throw e;
        }
    }

    @Override
    public void rm(String file) throws SftpException {
        if (StringUtils.isBlank(file)) {
            throw new IllegalArgumentException("remote cannot be blank");
        }

        try {
            sshSessionFactory.getSftpChannel().rm(file);
        } catch (JSchException ex) {
            error("rm", file);
        } catch (SftpException e) {
            error("rm", file);
            throw e;
        }
    }

    @Override
    public void rmdir(String dir) throws SftpException {
        if (StringUtils.isBlank(dir)) {
            throw new IllegalArgumentException("dir cannot be blank");
        }

        try {
            sshSessionFactory.getSftpChannel().rmdir(dir);
        } catch (JSchException ex) {
            error("cd", dir);
        } catch (SftpException e) {
            error("cd", dir);
            throw e;
        }
    }

    @Override
    public boolean notExists(String part) throws SftpException, JSchException {
        if (StringUtils.isBlank(part)) {
            throw new IllegalArgumentException("part cannot be blank");
        }

        try {
            SftpATTRS attrs = sshSessionFactory.getSftpChannel().lstat(part);
            if (attrs != null && !attrs.isDir()) {
                return true;
            }
            return false;
        } catch (SftpException ex) {
            error("notExists", part);
            throw ex;
        }
    }

    private void error(String ops, String path) {
        try {
            log.error("Operation: " + ops + ", pwd:" + sshSessionFactory.getSftpChannel().pwd() + ", path: " + path);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

}
