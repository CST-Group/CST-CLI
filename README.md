# The Cognitive System Command Line Interface - CSCLI

This is a command line interface (CLI) to easily create [CST projects](https://cst.fee.unicamp.br).

## Installation

Download the deb (Debian/Ubuntu) or rpm package from the latest release, install it on your machine and add the executable to your `PATH` variable
```shell
echo 'export PATH="$PATH:/opt/cst-cli/bin"'
source ~/.bashrc
```


**DEB package**
```shell
sudo apt install ./cst_cli_x.x.deb
OR
sudo dpkg -i ./cst_cli_x.x.deb
```

**RPM package**
```shell
sudo rpm -r ./cst_cli_v_x.x.rpm
```



### Local compile

To compile and package the CLI locally is necessary to have [Docker](https://docs.docker.com/desktop/) install on your machine.

1. Clone this repository into a local folder
    ```shell
   git clone https://github.com/brgsil/CST-CLI.git
   ```
2. Go into the repository directory and run the package script
   ```shell
   cd CST-CLI
   ./package.sh
   ```
3. Install deb or rpm package from `build/cst_cli` folder
4. Update `PATH` variable to include executable
   ```shell
   echo 'export PATH="$PATH:/opt/cst-cli/bin"' >> ~/.bashrc
   source ~/.bashrc
   ```

## Example

To create a new example CST project run the command:
```shell
mkdir TestProject
cd TestProject
cst init -f ../test.yaml
```
