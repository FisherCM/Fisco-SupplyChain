#!/bin/bash 

function usage() 
{
    echo " Usage : "
    echo "   bash asset_run.sh deploy"
    echo "   bash asset_run.sh query fromAccount toAccount item "
    echo "   bash asset_run.sh create fromAccount toAccount item amount "
    echo "   bash asset_run.sh pass issuer from_account from_item to_account to_item amount "
    echo "   bash asset_run.sh bank from_account to_account item "
    echo "   bash asset_run.sh redeem from_account to_account item "
    echo " "
    echo " "
    echo "examples : "
    echo "   bash asset_run.sh deploy "
    echo "   bash asset_run.sh query  Bob  Alice apple "
    echo "   bash asset_run.sh create  Bob  Alice apple 100 "
    echo "   bash asset_run.sh pass  Alice Bob apple Cindy ring 100 "
    echo "   bash asset_run.sh bank Bob Alice apple"
    echo "   bash asset_run.sh redeem Bob Alice apple"
    exit 0
}

    case $1 in
    deploy)
            [ $# -lt 1 ] && { usage; }
            ;;
    create)
            [ $# -lt 5 ] && { usage; }
            ;;
    pass)
            [ $# -lt 7 ] && { usage; }
            ;;
    query)
            [ $# -lt 4 ] && { usage; }
            ;;
    bank)
            [ $# -lt 4 ] && { usage; }
            ;;
    redeem)
            [ $# -lt 4 ] && { usage; }
            ;;
    *)
        usage
            ;;
    esac

    java -Djdk.tls.namedGroups="secp256k1" -cp 'apps/*:conf/:lib/*' org.fisco.bcos.asset.client.AssetClient $@

