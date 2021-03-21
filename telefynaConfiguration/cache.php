<?php
    $now = date('Y-m-d_H:i:s');
    file_put_contents("exports/".$now."-config.json", $_POST['config']);
    file_put_contents("exports/".$now."-loc.txt", $_POST['loc']);
?>