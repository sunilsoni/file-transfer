package com.ssh.remote;

import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.exception.SftpException;

public interface RemoteFileOperation {

    void mkdir(String part) throws SftpException, JSchException;

    void cd(String part) throws SftpException, JSchException;

    void rm(String file) throws SftpException, JSchException;

    void rmdir(String dir) throws SftpException, JSchException;

    boolean notExists(String part) throws SftpException, JSchException;


}
