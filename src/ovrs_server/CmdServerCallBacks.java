/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ovrs_server;

/**
 *
 * @author mreddy3
 */
public interface CmdServerCallBacks {
    public void onCmdServerStarted(int port);
    public void onCmdServerStopped();
    public void onCmdServerError();
}
